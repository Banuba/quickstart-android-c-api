
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

namespace
{
    struct banuba_sdk_manager
    {
        offscreen_effect_player_sptr oep{nullptr};

        banuba_sdk_manager(const std::vector<std::string>& path_to_resources, const std::string& client_token)
        {
            /* Create instance of render_context */
            auto rc = bnb::oep::interfaces::render_context::create();

            /* Create an instance of our offscreen_render_target implementation, you can use your own.
             * pass render_context */
            auto ort = bnb::oep::interfaces::offscreen_render_target::create(rc);

            /* Create an instance of effect_player implementation with cpp api, pass path to location of
             * effects and client token */
            auto ep = bnb::oep::interfaces::effect_player::create(path_to_resources, client_token);

            /* Create instance of offscreen_effect_player, pass effect_player, offscreen_render_target
             * and dimension of processing frame (for best performance it is better to coincide
             * with camera frame dimensions) */
            oep = bnb::oep::interfaces::offscreen_effect_player::create(ep, ort, 1, 1);
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

    offscreen_effect_player_sptr get_offscreen_effect_player_from_jlong(jlong jsdk)
    {
        auto oep = reinterpret_cast<banuba_sdk_manager*>(jsdk)->oep;
        if (oep == nullptr) {
            print_message("error: get_offscreen_effect_player_from_jlong(): oep == nullptr\n");
        }
        return oep;
    }
} /* namespace */


extern "C"
{
    /* OffscreenEffectPlayerexternalCreate - kotlin interface */
    JNIEXPORT jlong JNICALL Java_com_banuba_sdk_example_quickstart_1c_1api_OffscreenEffectPlayer_externalCreate(JNIEnv* env, jobject thiz, jstring jpath_to_resources, jstring jtoken)
    {
        std::vector<std::string> path{jstring_to_string(env, jpath_to_resources)};
        auto token = jstring_to_string(env, jtoken);
        auto oep = new banuba_sdk_manager(path, token);
        return reinterpret_cast<jlong>(oep);
    }

    /* OffscreenEffectPlayer::externalDestroy - kotlin interface */
    JNIEXPORT void JNICALL Java_com_banuba_sdk_example_quickstart_1c_1api_OffscreenEffectPlayer_externalDestroy(JNIEnv* env, jobject thiz, jlong jsdk)
    {
        auto sdk = reinterpret_cast<banuba_sdk_manager*>(jsdk);
        if (sdk == nullptr) {
            print_message("error: OffscreenEffectPlayer::externalDestroy(): sdk == nullptr\n");
            return;
        }
        delete sdk;
    }

    /* OffscreenEffectPlayer::externalProcessImageAsync - kotlin interface */
    JNIEXPORT void JNICALL Java_com_banuba_sdk_example_quickstart_1c_1api_OffscreenEffectPlayer_externalProcessImageAsync(JNIEnv* env, jobject thiz, jlong jsdk, jobject jimage, jint jwidth, jint jheight)
    {
        auto oep = get_offscreen_effect_player_from_jlong(jsdk);
        if (oep == nullptr) {
            return;
        }

        print_message("push frame");
        auto width = static_cast<int32_t>(jwidth);
        auto height = static_cast<int32_t>(jheight);

        auto* input_image_data = static_cast<uint8_t*>(env->GetDirectBufferAddress(jimage));
        int y_size = width * height;
        int uv_size = width * height / 4;

        auto * y_input_image_data = input_image_data;
        auto * u_input_image_data = input_image_data + y_size;
        auto * v_input_image_data = u_input_image_data + uv_size;

        /* Create an image */
        using ns_pb = bnb::oep::interfaces::pixel_buffer;
        ns_pb::plane_sptr y_plane_data(y_input_image_data, [](uint8_t*) { /* DO NOTHING */ });
        ns_pb::plane_sptr u_plane_data(u_input_image_data, [](uint8_t*) { /* DO NOTHING */ });
        ns_pb::plane_sptr v_plane_data(v_input_image_data, [](uint8_t*) { /* DO NOTHING */ });

        ns_pb::plane_data y_plane{y_plane_data, static_cast<size_t>(y_size), width};
        ns_pb::plane_data u_plane{u_plane_data, static_cast<size_t>(uv_size), width / 2};
        ns_pb::plane_data v_plane{v_plane_data, static_cast<size_t>(uv_size), width / 2};

        std::vector<ns_pb::plane_data> planes{y_plane, u_plane, v_plane};
        auto format = bnb::oep::interfaces::image_format::i420_bt709_full;
//        auto format = bnb::oep::interfaces::image_format::i420_bt601_full;
        auto pb_image = ns_pb::create(planes, format, width, height, [](auto* pb) {});

        JavaVM* jvm;
        env->GetJavaVM(&jvm);
        jobject this_ref = env->NewGlobalRef(thiz);

        // Callback for received pixel buffer from the offscreen effect player
        auto get_pixel_buffer_callback = [this_ref, jvm](image_processing_result_sptr result) {
            if (result != nullptr) {
                // Callback for update data in render thread
                auto get_image_callback = [this_ref, jvm](pixel_buffer_sptr image) {
                    if (image == nullptr) {
                        return;
                    }
                    JNIEnv* env = nullptr;

                    // double check it's all ok
                    int getEnvStat = jvm->GetEnv((void**) &env, JNI_VERSION_1_6);
                    if (getEnvStat == JNI_EDETACHED) {
                        if (jvm->AttachCurrentThread((JNIEnv**) &env, nullptr) != JNI_OK) {
                            print_message("GetEnv: Failed to attach");
                            return;
                        }
                    } else if (getEnvStat == JNI_EVERSION) {
                        print_message("GetEnv: version not supported");
                        return;
                    }

                    jclass jcallback_class = env->GetObjectClass(this_ref);
                    jmethodID jcallback_method = env->GetMethodID(jcallback_class, "dataReady", "([BII)V");

                    auto size = image->get_width() * image->get_height() * image->get_bytes_per_pixel();
                    auto byte_array = env->NewByteArray(size);
                    env->SetByteArrayRegion(byte_array, 0, size, reinterpret_cast<const jbyte*>((uint8_t*) image->get_base_sptr().get()));

                    // call callback
                    env->CallVoidMethod(this_ref, jcallback_method, byte_array, image->get_width(), image->get_height());

                    if (env->ExceptionCheck()) {
                        env->ExceptionDescribe();
                    }
                    env->DeleteGlobalRef(this_ref);

                    jvm->DetachCurrentThread();
                };
                // Get image from effect_player and return it in the callback
                result->get_image(bnb::oep::interfaces::image_format::bpc8_rgba, get_image_callback);
            }
        };
        oep->process_image_async(pb_image, bnb::oep::interfaces::rotation::deg0, get_pixel_buffer_callback, bnb::oep::interfaces::rotation::deg90);
    }

    /* OffscreenEffectPlayer::externalSurfaceChanged - kotlin interface */
    JNIEXPORT void JNICALL Java_com_banuba_sdk_example_quickstart_1c_1api_OffscreenEffectPlayer_externalSurfaceChanged(JNIEnv* env, jobject thiz, jlong jsdk, jint jwidth, jint jheight)
    {
        auto oep = get_offscreen_effect_player_from_jlong(jsdk);
        if (oep == nullptr) {
            return;
        }
        auto width = static_cast<int32_t>(jwidth);
        auto height = static_cast<int32_t>(jheight);
        oep->surface_changed(width, height);
    }

    /* OffscreenEffectPlayer::externalLoadEffect - kotlin interface */
    JNIEXPORT void JNICALL Java_com_banuba_sdk_example_quickstart_1c_1api_OffscreenEffectPlayer_externalLoadEffect(JNIEnv* env, jobject thiz, jlong jsdk, jstring jpath)
    {
        auto oep = get_offscreen_effect_player_from_jlong(jsdk);
        if (oep == nullptr) {
            return;
        }
        auto path = jstring_to_string(env, jpath);
        oep->load_effect(path);
    }

    /* OffscreenEffectPlayer::externalUnloadEffect - kotlin interface */
    JNIEXPORT void JNICALL Java_com_banuba_sdk_example_quickstart_1c_1api_OffscreenEffectPlayer_externalUnloadEffect(JNIEnv* env, jobject thiz, jlong jsdk)
    {
        auto oep = get_offscreen_effect_player_from_jlong(jsdk);
        if (oep == nullptr) {
            return;
        }
        oep->unload_effect();
    }

    /* OffscreenEffectPlayer::externalPause - kotlin interface */
    JNIEXPORT void JNICALL Java_com_banuba_sdk_example_quickstart_1c_1api_OffscreenEffectPlayer_externalPause(JNIEnv* env, jobject thiz, jlong jsdk)
    {
        auto oep = get_offscreen_effect_player_from_jlong(jsdk);
        if (oep == nullptr) {
            return;
        }
        oep->pause();
    }

    /* OffscreenEffectPlayer::externalResume - kotlin interface */
    JNIEXPORT void JNICALL Java_com_banuba_sdk_example_quickstart_1c_1api_OffscreenEffectPlayer_externalResume(JNIEnv* env, jobject thiz, jlong jsdk)
    {
        auto oep = get_offscreen_effect_player_from_jlong(jsdk);
        if (oep == nullptr) {
            return;
        }
        oep->resume();
    }

    /* OffscreenEffectPlayer::externalCallJsMethod - kotlin interface */
    JNIEXPORT void JNICALL Java_com_banuba_sdk_example_quickstart_1c_1api_OffscreenEffectPlayer_externalCallJsMethod(JNIEnv* env, jobject thiz, jlong jsdk, jstring jmethod, jstring jparams)
    {
        auto oep = get_offscreen_effect_player_from_jlong(jsdk);
        if (oep == nullptr) {
            return;
        }
        auto method = jstring_to_string(env, jmethod);
        auto params = jstring_to_string(env, jparams);
        oep->call_js_method(method, params);
    }
} /* extern "C" */
