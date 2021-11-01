
#include <bnb/effect_player.h>
#include <bnb/error.h>
#include <bnb/utility_manager.h>

#include "offscreen_effect_player.hpp"
#include "offscreen_render_target.hpp"
#include "thread_pool.h"

#include <android/log.h>
#include <jni.h>

#include <string>
#include <thread>
#include <vector>
#include <optional>


#define CHECK_ERROR(error)                                  \
    do {                                                    \
        if (error) {                                        \
            std::string msg = bnb_error_get_message(error); \
            bnb_error_destroy(error);                       \
            throw std::runtime_error(msg);                  \
        }                                                   \
    } while (false);

namespace
{
    std::string jstring2string(JNIEnv* env, jstring jstr)
    {
        const char* chars = env->GetStringUTFChars(jstr, NULL);
        std::string ret(chars);
        env->ReleaseStringUTFChars(jstr, chars);
        return ret;
    }

    struct BanubaSdkManager
    {
        ioep_sptr oep;

        BanubaSdkManager(const std::vector<std::string>& path_to_resources, const std::string& client_token)
        {
            // Size of photo.jpg
            int32_t width = 1000;
            int32_t height = 1500;
            bnb_effect_player_configuration_t ep_cfg{width, height, bnb_nn_mode_automatically, bnb_good, false, false};
            auto ort = std::make_shared<bnb::offscreen_render_target>(width, height);
            oep = bnb::interfaces::offscreen_effect_player::create(path_to_resources, client_token,
                width, height, false, ort);
        }

        ~BanubaSdkManager() {}
    };
} // namespace


extern "C" JNIEXPORT jlong JNICALL
Java_com_banuba_sdk_example_quickstart_1cpp_BanubaSdk_createEffectPlayer(JNIEnv* env, jobject thiz, jstring path_to_resources, jstring client_token)
{
    static BanubaSdkManager* sdk{nullptr};
    if(sdk == nullptr){
        std::vector<std::string> paths_r{jstring2string(env, path_to_resources)};
        auto token = jstring2string(env, client_token);
        sdk = new BanubaSdkManager(paths_r, token);
    }
    return (jlong) sdk;
}

extern "C" JNIEXPORT void JNICALL
Java_com_banuba_sdk_example_quickstart_1cpp_BanubaSdk_destroyEffectPlayer(JNIEnv* env, jobject thiz, jlong effect_player)
{
    auto sdk = (BanubaSdkManager*) effect_player;
    delete sdk;
}

extern "C" JNIEXPORT void JNICALL
Java_com_banuba_sdk_example_quickstart_1cpp_BanubaSdk_loadEffect(JNIEnv* env, jobject thiz, jlong effect_player, jstring name)
{
    auto sdk = (BanubaSdkManager*) effect_player;
    auto effect_name = jstring2string(env, name);

    sdk->oep->load_effect(effect_name);
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_banuba_sdk_example_quickstart_1cpp_BanubaSdk_processPhoto(JNIEnv* env, jobject thiz, jlong effect_player, jobject rgba, jint width, jint height)
{
    auto sdk = (BanubaSdkManager*) effect_player;

    auto* data = static_cast<uint8_t*>(env->GetDirectBufferAddress(rgba));
    bnb_image_format_t image_format{
                static_cast<uint32_t>(width),
                static_cast<uint32_t>(height),
                bnb_image_orientation_t::BNB_DEG_0,
                /* is_mirrored */ false,
                /* face_orientation */ 0};
    int32_t image_stride = static_cast<int32_t>(width) * 4 /* rgba is 1 byte per chanel. */;
    auto rgb_in = std::make_shared<image_wrapper>(image_format, bnb_pixel_format_t::BNB_RGBA, data, image_stride);
    auto pb = sdk->oep->process_image_rgba(rgb_in, std::nullopt);
    auto rgba_out_opt = pb->get_rgba();

    if (!rgba_out_opt.has_value()) {
        throw std::runtime_error("Failed get_rgba from OEP");
    }

    auto rgba_out = *rgba_out_opt;

    auto size = rgba_out.get_format().width * rgba_out.get_format().height * rgba_out.bytes_per_pixel()/*rgba_out.bytes_per_pixel()*/;
    auto byte_array = env->NewByteArray(size);
    env->SetByteArrayRegion(byte_array, 0, size, reinterpret_cast<const jbyte*>(rgba_out.get_rgb_data_ptr()));
    return byte_array;
}

extern "C" JNIEXPORT void JNICALL
Java_com_banuba_sdk_example_quickstart_1cpp_BanubaSdk_surfaceChanged(JNIEnv* env, jobject thiz, jlong effect_player, jint width, jint height)
{
    auto sdk = (BanubaSdkManager*) effect_player;

    sdk->oep->surface_changed(width, height);
}
