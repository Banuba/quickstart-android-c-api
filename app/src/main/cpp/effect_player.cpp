#include "effect_player.hpp"

#include <android/log.h>

#include <thread>
#include <optional>
#include <iostream>
#include <algorithm>

namespace
{
    void print_message(const char* message)
    {
        __android_log_print(ANDROID_LOG_ERROR, "OEP_ERROR ", "%s\n", message);
    }

    void check_error(bnb_error* e)
    {
        if (e) {
            std::string msg = bnb_error_get_message(e);
            bnb_error_destroy(e);
            throw std::runtime_error(msg);
        }
    }
}


namespace bnb::oep
{

    /* effect_player::create */
    effect_player_sptr interfaces::effect_player::create(int32_t width, int32_t height)
    {
        return std::make_shared<bnb::oep::effect_player>(width, height);
    }

    /* effect_player::effect_player CONSTRUCTOR */
    effect_player::effect_player(int32_t width, int32_t height)
    {
        bnb_error* error{nullptr};
        bnb_effect_player_configuration_t ep_cfg{width, height, bnb_nn_mode_enable, bnb_good, true, false};
        m_ep = bnb_effect_player_create(&ep_cfg, &error);
        check_error(error);
        bnb_effect_player_set_render_consistency_mode(m_ep, bnb_consistency_mode_synchronous, &error);
        check_error(error);
        if (m_ep == nullptr) {
            print_message("Failed to create effect player holder");
        }

        auto config = bnb_processor_configuration_create(nullptr);

        bnb_processor_configuration_set_use_future_filter(config, false, nullptr);
        m_fp = bnb_frame_processor_create_realtime_processor(bnb_realtime_processor_mode_sync, config, &error);
        check_error(error);

        bnb_effect_player_set_frame_processor(m_ep, m_fp, &error);
        check_error(error);

        bnb_processor_configuration_destroy(config, nullptr);
    }

    /* effect_player::~effect_player */
    effect_player::~effect_player()
    {
        if (m_ep) {
            bnb_effect_player_destroy(m_ep, nullptr);
            m_ep = nullptr;
        }
        if (m_fp) {
            bnb_frame_processor_destroy(m_fp, nullptr);
            m_fp = nullptr;
        }
    }

    /* effect_player::surface_created */
    void effect_player::surface_created(int32_t width, int32_t height)
    {
        bnb_effect_player_surface_created(m_ep, width, height, nullptr);
        effect_manager_holder_t* em = bnb_effect_player_get_effect_manager(m_ep, nullptr);
        bnb_effect_manager_set_effect_size(em, width, height, nullptr);
    }

    /* effect_player::surface_changed */
    void effect_player::surface_changed(int32_t width, int32_t height)
    {
        bnb_effect_player_surface_changed(m_ep, width, height, nullptr);
        effect_manager_holder_t* em = bnb_effect_player_get_effect_manager(m_ep, nullptr);
        bnb_effect_manager_set_effect_size(em, width, height, nullptr);
    }

    /* effect_player::surface_destroyed */
    void effect_player::surface_destroyed()
    {
        bnb_effect_player_surface_destroyed(m_ep, nullptr);
    }

    /* effect_player::load_effect */
    bool effect_player::load_effect(const std::string& effect)
    {
        if (auto e_manager = bnb_effect_player_get_effect_manager(m_ep, nullptr)) {
            bnb_effect_manager_load_effect(e_manager, effect.c_str(), nullptr);
            return true;
        }
        print_message("effect manager not initialized");
        return false;
    }

    /* effect_player::call_js_method */
    bool effect_player::call_js_method(const std::string& method, const std::string& param)
    {
        if (auto e_manager = bnb_effect_player_get_effect_manager(m_ep, nullptr)) {
            if (auto effect = bnb_effect_manager_get_current_effect(e_manager, nullptr)) {
                bnb_effect_call_js_method(effect, method.c_str(), param.c_str(), nullptr);
            } else {
                print_message("effect not loaded");
                return false;
            }
        } else {
            print_message("effect manager not initialized");
            return false;
        }
        return true;
    }

    /* effect_player::eval_js */
    void effect_player::eval_js(const std::string& script, oep_eval_js_result_cb result_callback)
    {
        // Not implemented
    }

    /* effect_player::pause */
    void effect_player::pause()
    {
        bnb_effect_player_playback_pause(m_ep, nullptr);
    }

    /* effect_player::resume */
    void effect_player::resume()
    {
        bnb_effect_player_playback_play(m_ep, nullptr);
    }

    /* effect_player::push_frame */
    void effect_player::push_frame(pixel_buffer_sptr image, bnb::oep::interfaces::rotation image_orientation, bool require_mirroring)
    {
        full_image_holder_t* bnb_image{nullptr};

        using ns = bnb::oep::interfaces::image_format;
        auto bnb_image_format = make_bnb_image_format(image, image_orientation, require_mirroring);
        switch (image->get_image_format()) {
            case ns::bpc8_rgb:
            case ns::bpc8_bgr:
            case ns::bpc8_rgba:
            case ns::bpc8_bgra:
            case ns::bpc8_argb:
                bnb_image = bnb_full_image_from_bpc8_img(
                    bnb_image_format,
                    make_bnb_pixel_format(image),
                    image->get_base_sptr().get(),
                    image->get_bytes_per_row(),
                    nullptr);
                break;
            case ns::nv12_bt601_full:
            case ns::nv12_bt601_video:
            case ns::nv12_bt709_full:
            case ns::nv12_bt709_video:
                bnb_image = bnb_full_image_from_yuv_nv12_img(
                    bnb_image_format,
                    image->get_base_sptr_of_plane(0).get(),
                    image->get_bytes_per_row_of_plane(0),
                    image->get_base_sptr_of_plane(1).get(),
                    image->get_bytes_per_row_of_plane(1),
                    nullptr);
                break;
            case ns::i420_bt601_full:
            case ns::i420_bt601_video:
            case ns::i420_bt709_full:
            case ns::i420_bt709_video:
                bnb_image = bnb_full_image_from_yuv_i420_img(
                    bnb_image_format,
                    image->get_base_sptr_of_plane(0).get(),
                    image->get_bytes_per_row_of_plane(0),
                    1,
                    image->get_base_sptr_of_plane(1).get(),
                    image->get_bytes_per_row_of_plane(1),
                    1,
                    image->get_base_sptr_of_plane(2).get(),
                    image->get_bytes_per_row_of_plane(2),
                    1,
                    nullptr);
                break;
            default:
                break;
        }

        if (!bnb_image) {
            print_message("no image was created");
            return;
        }

        bnb_error* error{nullptr};
        auto fd = bnb_frame_data_init(&error);
        check_error(error);

        bnb_frame_data_add_full_img(fd, bnb_image, &error);
        check_error(error);

        bnb_frame_processor_push(m_fp, fd, &error);
        check_error(error);

        bnb_frame_data_release(fd, nullptr);
        bnb_full_image_release(bnb_image, nullptr);
    }

    /* effect_player::draw */
    int64_t effect_player::draw()
    {
        bnb_error* error{nullptr};
        int64_t ret = -1;

        auto result = bnb_frame_processor_pop(m_fp, &error);
        check_error(error);
        if (result.status != bnb_processor_status_ok) {
            bnb_frame_data_release(result.frame_data, nullptr);
            return -1;
        }

        ret = bnb_effect_player_draw_with_external_frame_data(m_ep, result.frame_data, &error);

        bnb_frame_data_release(result.frame_data, nullptr);

        check_error(error);

        return ret;
    }

    /* effect_player::make_bnb_image_format */
    bnb_image_format_t effect_player::make_bnb_image_format(pixel_buffer_sptr image, interfaces::rotation orientation, bool require_mirroring)
    {
        bnb_image_orientation_t camera_orient{BNB_DEG_0};
        using ns = bnb::oep::interfaces::rotation;
        switch (orientation) {
            case ns::deg0:
                break;
            case ns::deg90:
                camera_orient = BNB_DEG_90;
                break;
            case ns::deg180:
                camera_orient = BNB_DEG_180;
                break;
            case ns::deg270:
                camera_orient = BNB_DEG_270;
                break;
        }
        return {static_cast<uint32_t>(image->get_width()), static_cast<uint32_t>(image->get_height()), camera_orient, require_mirroring, 0};
    }

    /* effect_player::make_bnb_pixel_format */
    bnb_pixel_format_t effect_player::make_bnb_pixel_format(pixel_buffer_sptr image)
    {
        bnb_pixel_format_t fmt{BNB_RGB};
        using ns = bnb::oep::interfaces::image_format;
        switch (image->get_image_format()) {
            case ns::bpc8_rgb:
                break;
            case ns::bpc8_bgr:
                fmt = BNB_BGR;
                break;
            case ns::bpc8_rgba:
                fmt = BNB_RGBA;
                break;
            case ns::bpc8_bgra:
                fmt = BNB_BGRA;
                break;
            case ns::bpc8_argb:
                fmt = BNB_ARGB;
                break;
            default:
                break;
        }
        return fmt;
    }

    void effect_player::stop() {
        bnb_effect_player_playback_stop(m_ep, nullptr);
    }

} /* namespace bnb::oep */
