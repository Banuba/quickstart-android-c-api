
#include <bnb/effect_player.h>
#include <bnb/error.h>
#include <bnb/utility_manager.h>

#include "effect_player.hpp"
#include "render_context.hpp"
#include "oep/interfaces/pixel_buffer.hpp"
#include "oep/interfaces/image_format.hpp"
#include "oep/interfaces/offscreen_effect_player.hpp"
#include "oep/interfaces/offscreen_render_target.hpp"

#include <android/log.h>
#include <jni.h>

#include <string>
#include <vector>

#include <stdio.h>
#include <libyuv.h>

namespace
{
    struct image_info {
        int width;
        int height;
        int row_stride0;
        int row_stride1;
        int row_stride2;
        int pixel_stride0;
        int pixel_stride1;
        int pixel_stride2;
        int input_orientation;
        int output_orientation;
        int pixel_format;
        int image_format;
        bool require_mirroring;
    };

    int get_int_field(const char* field_name, JNIEnv* env, jobject object, jclass object_class)
    {
        jfieldID field_id = env->GetFieldID(object_class, field_name, "I");
        return env->GetIntField(object, field_id);
    }

    bool get_bool_field(const char* field_name, JNIEnv* env, jobject object, jclass object_class)
    {
        jfieldID field_id = env->GetFieldID(object_class, field_name, "Z");
        return env->GetBooleanField(object, field_id);
    }

    image_info get_image_info(JNIEnv* env, jobject image_info)
    {
        jclass cls = env->GetObjectClass(image_info);
        return {
                get_int_field("width", env, image_info, cls),
                get_int_field("height", env, image_info, cls),
                get_int_field("rowStride0", env, image_info, cls),
                get_int_field("rowStride1", env, image_info, cls),
                get_int_field("rowStride2", env, image_info, cls),
                get_int_field("pixelStride0", env, image_info, cls),
                get_int_field("pixelStride1", env, image_info, cls),
                get_int_field("pixelStride2", env, image_info, cls),
                get_int_field("inputOrientation", env, image_info, cls),
                get_int_field("outputOrientation", env, image_info, cls),
                get_int_field("pixelFormat", env, image_info, cls),
                get_int_field("imageFormat", env, image_info, cls),
                get_bool_field("requireMirroring", env, image_info, cls)
        };
    }
    
    bnb::oep::interfaces::image_format get_image_format(int output_image_format) {
        switch (output_image_format) {
            case 1:
                return bnb::oep::interfaces::image_format::nv12_bt601_full;
            case 2:
                return bnb::oep::interfaces::image_format::i420_bt601_full;
            default:
                return bnb::oep::interfaces::image_format::bpc8_rgba;
        }
    }

    std::vector<bnb::oep::interfaces::pixel_buffer::plane_data> create_planes_from_format(
            uint8_t* input_image_data0,
            uint8_t* input_image_data1,
            uint8_t* input_image_data2,
            bnb::oep::interfaces::image_format format,
            const image_info& image_info)
    {
        // only nv12_bt601_full and i420_bt601_full are supported
        assert(format == bnb::oep::interfaces::image_format::nv12_bt601_full
             ||format == bnb::oep::interfaces::image_format::i420_bt601_full);

        using ns_pb = bnb::oep::interfaces::pixel_buffer;
        if(format == bnb::oep::interfaces::image_format::nv12_bt601_full) {
            int y_size = image_info.row_stride0  * image_info.height;
            int uv_size = image_info.row_stride1 * image_info.height / 2;

            ns_pb::plane_sptr y_plane_data(new ns_pb::plane_sptr::element_type[y_size], [](uint8_t* ptr) { delete[] ptr; });
            std::memcpy(y_plane_data.get(), input_image_data0, y_size);

            ns_pb::plane_sptr uv_plane_data(new ns_pb::plane_sptr::element_type[uv_size], [](uint8_t* ptr) { delete[] ptr; });
            std::memcpy(uv_plane_data.get(), input_image_data1, uv_size);

            ns_pb::plane_data y_plane{std::move(y_plane_data), static_cast<size_t>(y_size), image_info.row_stride0};
            ns_pb::plane_data uv_plane{std::move(uv_plane_data), static_cast<size_t>(uv_size), image_info.row_stride1};
            std::vector<ns_pb::plane_data> planes{std::move(y_plane), std::move(uv_plane)};
            return planes;
        }

        if(format == bnb::oep::interfaces::image_format::i420_bt601_full) {
            int y_size = image_info.row_stride0  * image_info.height;
            int u_size = image_info.row_stride1 * image_info.height / 4;
            int v_size = image_info.row_stride2 * image_info.height/ 4;

            ns_pb::plane_sptr y_plane_data(new ns_pb::plane_sptr::element_type[y_size], [](uint8_t* ptr) { delete[] ptr;});
            std::memcpy(y_plane_data.get(), input_image_data0, y_size);

            ns_pb::plane_sptr u_plane_data(new ns_pb::plane_sptr::element_type[u_size], [](uint8_t* ptr) { delete[] ptr; });
            ns_pb::plane_sptr v_plane_data(new ns_pb::plane_sptr::element_type[v_size], [](uint8_t* ptr) { delete[] ptr; });

            auto ptr_u = u_plane_data.get();
            auto ptr_v = v_plane_data.get();

            for (unsigned row = 0; row < image_info.height * image_info.row_stride1 / 2; row += 2) {
                *ptr_u++ = input_image_data1[row];
                *ptr_v++ = input_image_data1[row + 1];
            }

            ns_pb::plane_data y_plane{std::move(y_plane_data), static_cast<size_t>(y_size), image_info.row_stride0};
            ns_pb::plane_data u_plane{std::move(u_plane_data), static_cast<size_t>(u_size), image_info.row_stride1 / 2};
            ns_pb::plane_data v_plane{std::move(v_plane_data), static_cast<size_t>(v_size), image_info.row_stride2 / 2};
            std::vector<ns_pb::plane_data> planes{std::move(y_plane), std::move(u_plane), std::move(v_plane)};
            return planes;
        }

        return {};
    }

    pixel_buffer_sptr create_pixel_buffer(JNIEnv* env, jobject jimageY, jobject jimageU, jobject jimageV,
                                          const image_info& image_info, bnb::oep::interfaces::image_format image_format)
    {
        uint8_t* input_image_data0 = static_cast<uint8_t*>(env->GetDirectBufferAddress(jimageY));
        uint8_t* input_image_data1 = static_cast<uint8_t*>(env->GetDirectBufferAddress(jimageU));
        uint8_t* input_image_data2 = static_cast<uint8_t*>(env->GetDirectBufferAddress(jimageV));
        auto width = static_cast<int32_t>(image_info.width);
        auto height = static_cast<int32_t>(image_info.height);
        auto planes = create_planes_from_format(input_image_data0, input_image_data1, input_image_data2, image_format, image_info);
        return bnb::oep::interfaces::pixel_buffer::create(planes, image_format, width, height, [](auto* pb) { delete pb; });
    }

    struct banuba_sdk_manager
    {
        offscreen_effect_player_sptr oep{nullptr};

        banuba_sdk_manager(int width, int height)
        {
            /* Create instance of render_context */
            auto rc = bnb::oep::interfaces::render_context::create();

            /* Create an instance of our offscreen_render_target implementation, you can use your own.
             * pass render_context */
            auto ort = bnb::oep::interfaces::offscreen_render_target::create(rc);

            /* Create an instance of effect_player implementation with c api */
            auto ep = bnb::oep::interfaces::effect_player::create(width, height);

            /* Create instance of offscreen_effect_player, pass effect_player, offscreen_render_target
             * and dimension of processing frame (for best performance it is better to coincide
             * with camera frame dimensions) */
            oep = bnb::oep::interfaces::offscreen_effect_player::create(ep, ort, width, height);
        }

        ~banuba_sdk_manager() = default;
    }; /* struct banuba_sdk_manager */

    std::string jstring_to_string(JNIEnv* env, jstring jstr)
    {
        const char* chars = env->GetStringUTFChars(jstr, NULL);
        std::string ret(chars);
        env->ReleaseStringUTFChars(jstr, chars);
        return ret;
    }

    bnb::oep::interfaces::rotation java_rotation_to_oep_rotation(int rotation)
    {
        switch (rotation) {
            case 0:
                return bnb::oep::interfaces::rotation::deg0;
            case 90:
                return bnb::oep::interfaces::rotation::deg90;
            case 180:
                return bnb::oep::interfaces::rotation::deg180;
            case 270:
                return bnb::oep::interfaces::rotation::deg270;
            default:
                print_message("error: java_rotation_to_oep_rotation() error: incorrect rotation\n");
                return bnb::oep::interfaces::rotation::deg0;
        }
    }

    offscreen_effect_player_sptr get_offscreen_effect_player_from_jlong(jlong jsdk)
    {
        auto oep = reinterpret_cast<banuba_sdk_manager*>(jsdk)->oep;
        if (oep == nullptr) {
            print_message("error: get_offscreen_effect_player_from_jlong(): oep == nullptr\n");
        }
        return oep;
    }

    jobject get_image_from_pixel_buffer(pixel_buffer_sptr image,
                                        bnb::oep::interfaces::image_format image_format,
                                        JNIEnv* env, jclass image_class) {
        jbyteArray byte_array0 = nullptr;
        jbyteArray byte_array1 = nullptr;
        jbyteArray byte_array2 = nullptr;
        switch(image_format) {
            case bnb::oep::interfaces::image_format::bpc8_rgba: {
                auto size = image->get_width() * image->get_height() * image->get_bytes_per_pixel();
                byte_array0 = env->NewByteArray(size);
                env->SetByteArrayRegion(byte_array0, 0, size, reinterpret_cast<const jbyte*>((uint8_t*) image->get_base_sptr().get()));
                break;
            }
            case bnb::oep::interfaces::image_format::nv12_bt601_full: {
                auto size0 = image->get_width() * image->get_height() * image->get_bytes_per_pixel();
                auto size1 = image->get_width() * image->get_height() * image->get_bytes_per_pixel() / 2;
                void* buf0 = reinterpret_cast<void*>((void*) image->get_base_sptr_of_plane(0).get());
                void* buf1 = reinterpret_cast<void*>((void*) image->get_base_sptr_of_plane(1).get());

                byte_array0 = env->NewByteArray(size0);
                byte_array1 = env->NewByteArray(size1);
                byte_array2 = env->NewByteArray(0);
                env->SetByteArrayRegion(byte_array0, 0, size0, reinterpret_cast<const jbyte*>(buf0));
                env->SetByteArrayRegion(byte_array1, 0, size1, reinterpret_cast<const jbyte*>(buf1));
                break;
            }
            case bnb::oep::interfaces::image_format::i420_bt601_full: {
                auto size0 = image->get_width_of_plane(0) * image->get_height() * image->get_bytes_per_pixel_of_plane(0);
                auto size1 = image->get_width_of_plane(1) * image->get_height() * image->get_bytes_per_pixel_of_plane(1);
                auto size2 = image->get_width_of_plane(2) * image->get_height() * image->get_bytes_per_pixel_of_plane(2);
                void* buf0 = reinterpret_cast<void*>((void*) image->get_base_sptr_of_plane(0).get());
                void* buf1 = reinterpret_cast<void*>((void*) image->get_base_sptr_of_plane(1).get());
                void* buf2 = reinterpret_cast<void*>((void*) image->get_base_sptr_of_plane(2).get());

                byte_array0 = env->NewByteArray(size0);
                byte_array1 = env->NewByteArray(size1);
                byte_array2 = env->NewByteArray(size2);
                env->SetByteArrayRegion(byte_array0, 0, size0, reinterpret_cast<const jbyte*>(buf0));
                int u_width = image->get_width_of_plane(1);
                int u_stride = image->get_bytes_per_row_of_plane(1);

                for(int i = 0; i < image->get_height_of_plane(1); ++i) {
                    env->SetByteArrayRegion(byte_array1, u_width * i, u_width, reinterpret_cast<const jbyte*>(buf1) + u_stride * i);
                }

                int v_width = image->get_width_of_plane(2);
                int v_stride = image->get_bytes_per_row_of_plane(2);

                for(int i = 0; i < image->get_height_of_plane(2); ++i) {
                    env->SetByteArrayRegion(byte_array2, v_width * i, v_width, reinterpret_cast<const jbyte*>(buf2) + v_stride * i);
                }
                break;
            }
            default:
                break;
        }

        if(byte_array0 == nullptr && byte_array1 == nullptr && byte_array2 == nullptr) {
            print_message("GetEnv: unsupported output image format");
            return nullptr;
        }
        auto image_constructor_id = env->GetMethodID(image_class, "<init>", "([B[B[BII)V");
        return env->NewObject(
                image_class, image_constructor_id, byte_array0, byte_array1, byte_array2,
                image->get_width(), image->get_height());
    }

    void draw_image_from_pixel_buffer(pixel_buffer_sptr pixel_buffer,
                                      bnb::oep::interfaces::image_format image_format,
                                      JNIEnv* env,
                                      jobject this_ref, jclass image_class_ref) {

        jobject image = get_image_from_pixel_buffer(std::move(pixel_buffer), image_format, env, image_class_ref);

        if(image == nullptr) {
            print_message("GetEnv: unsupported output image format");
            return;
        }

        jclass jcallback_class = env->GetObjectClass(this_ref);
        jmethodID jcallback_method = env->GetMethodID(jcallback_class,
                                                      "onDataReady", "(Lcom/banuba/quickstart_c_api/Image;)V");
        env->CallVoidMethod(this_ref, jcallback_method, image);

        if (env->ExceptionCheck()) {
            env->ExceptionDescribe();
        }
    }
} /* namespace */

extern "C"
{
    static utility_manager_holder_t* utility = nullptr;

    /* OffscreenEffectPlayer::externalInit - java interface */
    JNIEXPORT void JNICALL Java_com_banuba_quickstart_1c_1api_OffscreenEffectPlayer_externalInit(JNIEnv* env, jclass clazz, jstring jpath_to_resources, jstring jtoken)
    {
        assert(utility == nullptr);
        std::vector<std::string> path{jstring_to_string(env, jpath_to_resources)};
        auto token = jstring_to_string(env, jtoken);
        std::unique_ptr<const char*[]> res_paths = std::make_unique<const char*[]>(path.size() + 1);
        std::transform(path.begin(), path.end(), res_paths.get(), [](const auto& s) { return s.c_str(); });
        res_paths.get()[path.size()] = nullptr;
        utility = bnb_utility_manager_init(res_paths.get(), token.c_str(), nullptr);
    }

    /* OffscreenEffectPlayer::externalDeinit - java interface */
    JNIEXPORT void JNICALL Java_com_banuba_quickstart_1c_1api_OffscreenEffectPlayer_externalDeinit(JNIEnv* env, jclass clazz)
    {
        assert(utility != nullptr);
        bnb_utility_manager_release(utility, nullptr);
        utility = nullptr;
    }

    /* OffscreenEffectPlayer::externalCreate - java interface */
    JNIEXPORT jlong JNICALL Java_com_banuba_quickstart_1c_1api_OffscreenEffectPlayer_externalCreate(JNIEnv* env, jobject thiz, jint jwidth, jint jheight)
    {
        int32_t width = jwidth;
        int32_t height = jheight;
        auto oep = new banuba_sdk_manager(width, height);
        return reinterpret_cast<jlong>(oep);
    }

    /* OffscreenEffectPlayer::externalDestroy - java interface */
    JNIEXPORT void JNICALL Java_com_banuba_quickstart_1c_1api_OffscreenEffectPlayer_externalDestroy(JNIEnv* env, jobject thiz, jlong jsdk)
    {
        auto sdk = reinterpret_cast<banuba_sdk_manager*>(jsdk);
        if (sdk == nullptr) {
            print_message("error: OffscreenEffectPlayer::externalDestroy(): sdk == nullptr\n");
            return;
        }
        delete sdk;
    }

    /* OffscreenEffectPlayer::externalProcessImageAsync - java interface */
    JNIEXPORT void JNICALL Java_com_banuba_quickstart_1c_1api_OffscreenEffectPlayer_externalProcessImageAsync(
            JNIEnv* env, jobject thiz, jlong jsdk,
            jobject jimageY, jobject jimageU, jobject jimageV,
            jobject jimage_info, jboolean jis_process_image)
    {
        auto oep = get_offscreen_effect_player_from_jlong(jsdk);
        if (oep == nullptr) {
            return;
        }

        auto image_info = get_image_info(env, jimage_info);
        auto image_format = get_image_format(image_info.image_format);
        auto pb_image = create_pixel_buffer(env, jimageY, jimageU, jimageV, image_info, image_format);

        jclass image_class = env->FindClass("com/banuba/quickstart_c_api/Image");
        auto image_class_ref = (jclass) env->NewGlobalRef(image_class);

        jobject this_ref = env->NewGlobalRef(thiz);

        bool is_process_image = static_cast<int32_t>(jis_process_image);
        if(!is_process_image) {
            draw_image_from_pixel_buffer(pb_image, image_format, env, this_ref, image_class_ref);
            env->DeleteGlobalRef(image_class_ref);
            env->DeleteGlobalRef(this_ref);
            return;
        }

        JavaVM* jvm;
        env->GetJavaVM(&jvm);

        // Callback for received pixel buffer from the offscreen effect player
        auto get_pixel_buffer_callback = [this_ref, jvm, image_format, image_class_ref](image_processing_result_sptr result) {
            if (result != nullptr) {
                // Callback for update data in render thread
                auto get_image_callback = [this_ref, jvm, image_format, image_class_ref](pixel_buffer_sptr pb_image) {
                    JNIEnv* env = nullptr;

                    // double check it's all ok
                    int getEnvStat = jvm->GetEnv((void**) &env, JNI_VERSION_1_6);
                    if (getEnvStat == JNI_EDETACHED) {
                        if (jvm->AttachCurrentThread((JNIEnv**) &env, nullptr) != JNI_OK) {
                            print_message("GetEnv: Failed to attach");
                            env->DeleteGlobalRef(image_class_ref);
                            env->DeleteGlobalRef(this_ref);
                            return;
                        }
                    } else if (getEnvStat == JNI_EVERSION) {
                        print_message("GetEnv: version not supported");
                        env->DeleteGlobalRef(image_class_ref);
                        env->DeleteGlobalRef(this_ref);
                        return;
                    }

                    if (pb_image == nullptr) {
                        print_message("GetEnv: image is null");
                        env->DeleteGlobalRef(image_class_ref);
                        env->DeleteGlobalRef(this_ref);
                        jvm->DetachCurrentThread();
                        return;
                    }

                    draw_image_from_pixel_buffer(std::move(pb_image), image_format, env, this_ref,
                                                 image_class_ref);
                    env->DeleteGlobalRef(image_class_ref);
                    env->DeleteGlobalRef(this_ref);
                    jvm->DetachCurrentThread();
                };
                // Get image from effect_player and return it in the callback
                result->get_image(image_format, get_image_callback);
            }
        };
        auto in_rotation = java_rotation_to_oep_rotation(image_info.input_orientation);
        auto out_rotation = java_rotation_to_oep_rotation(image_info.output_orientation);
        oep->process_image_async(pb_image, in_rotation, image_info.require_mirroring, get_pixel_buffer_callback, out_rotation);
    }

    /* OffscreenEffectPlayer::externalSurfaceChanged - java interface */
    JNIEXPORT void JNICALL Java_com_banuba_quickstart_1c_1api_OffscreenEffectPlayer_externalSurfaceChanged(JNIEnv* env, jobject thiz, jlong jsdk, jint jwidth, jint jheight)
    {
        auto oep = get_offscreen_effect_player_from_jlong(jsdk);
        if (oep == nullptr) {
            return;
        }
        auto width = static_cast<int32_t>(jwidth);
        auto height = static_cast<int32_t>(jheight);
        oep->surface_changed(width, height);
    }

    /* OffscreenEffectPlayer::externalLoadEffect - java interface */
    JNIEXPORT void JNICALL Java_com_banuba_quickstart_1c_1api_OffscreenEffectPlayer_externalLoadEffect(JNIEnv* env, jobject thiz, jlong jsdk, jstring jpath)
    {
        auto oep = get_offscreen_effect_player_from_jlong(jsdk);
        if (oep == nullptr) {
            return;
        }
        auto path = jstring_to_string(env, jpath);
        oep->load_effect(path);
    }

    /* OffscreenEffectPlayer::externalUnloadEffect - java interface */
    JNIEXPORT void JNICALL Java_com_banuba_quickstart_1c_1api_OffscreenEffectPlayer_externalUnloadEffect(JNIEnv* env, jobject thiz, jlong jsdk)
    {
        auto oep = get_offscreen_effect_player_from_jlong(jsdk);
        if (oep == nullptr) {
            return;
        }
        oep->unload_effect();
    }

    /* OffscreenEffectPlayer::externalPause - java interface */
    JNIEXPORT void JNICALL Java_com_banuba_quickstart_1c_1api_OffscreenEffectPlayer_externalPause(JNIEnv* env, jobject thiz, jlong jsdk)
    {
        auto oep = get_offscreen_effect_player_from_jlong(jsdk);
        if (oep == nullptr) {
            return;
        }
        oep->pause();
    }

    /* OffscreenEffectPlayer::externalResume - java interface */
    JNIEXPORT void JNICALL Java_com_banuba_quickstart_1c_1api_OffscreenEffectPlayer_externalResume(JNIEnv* env, jobject thiz, jlong jsdk)
    {
        auto oep = get_offscreen_effect_player_from_jlong(jsdk);
        if (oep == nullptr) {
            return;
        }
        oep->resume();
    }

    /* OffscreenEffectPlayer::externalStop - java interface */
    JNIEXPORT void JNICALL Java_com_banuba_quickstart_1c_1api_OffscreenEffectPlayer_externalStop(JNIEnv* env, jobject thiz, jlong jsdk)
    {
        auto oep = get_offscreen_effect_player_from_jlong(jsdk);
        if (oep == nullptr) {
            return;
        }
        oep->stop();
    }

    /* OffscreenEffectPlayer::externalCallJsMethod - java interface */
    JNIEXPORT void JNICALL Java_com_banuba_quickstart_1c_1api_OffscreenEffectPlayer_externalCallJsMethod(JNIEnv* env, jobject thiz, jlong jsdk, jstring jmethod, jstring jparam)
    {
        auto oep = get_offscreen_effect_player_from_jlong(jsdk);
        if (oep == nullptr) {
            return;
        }
        auto method = jstring_to_string(env, jmethod);
        auto param = jstring_to_string(env, jparam);
        oep->call_js_method(method, param);
    }

    /* OffscreenEffectPlayer::externalEvalJs - java interface */
    JNIEXPORT void JNICALL Java_com_banuba_quickstart_1c_1api_OffscreenEffectPlayer_externalEvalJs(JNIEnv* env, jobject thiz, jlong jsdk, jstring jscript)
    {
        auto oep = get_offscreen_effect_player_from_jlong(jsdk);
        if (oep == nullptr) {
            return;
        }
        auto script = jstring_to_string(env, jscript);
        oep->eval_js(script, nullptr);
    }
} /* extern "C" */
