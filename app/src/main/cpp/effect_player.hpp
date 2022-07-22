#pragma once

#include <interfaces/effect_player.hpp>
#include <bnb/common_types.h>
#include <bnb/effect_player.h>
#include <bnb/utility_manager.h>

// Temporary solution. Normal logging will be added in the future
// TODO: add logging
void print_message(const char* message);

namespace bnb::oep
{

    class effect_player : public bnb::oep::interfaces::effect_player
    {
    public:
        effect_player(const std::vector<std::string>& path_to_resources, const std::string& client_token);

        ~effect_player();

        void surface_created(int32_t width, int32_t height) override;

        void surface_changed(int32_t width, int32_t height) override;

        void surface_destroyed() override;

        bool load_effect(const std::string& effect) override;

        bool call_js_method(const std::string& method, const std::string& param) override;

        void pause() override;

        void resume() override;

        void push_frame(pixel_buffer_sptr image, bnb::oep::interfaces::rotation image_orientation) override;

        void draw() override;

        void stop() override;

    private:
        bnb_image_format_t make_bnb_image_format(pixel_buffer_sptr image, interfaces::rotation orientation);
        bnb_pixel_format_t make_bnb_pixel_format(pixel_buffer_sptr image);
        void handle_bnb_error(bnb_error** error);

    private:
        utility_manager_holder_t* m_utility{nullptr};
        effect_player_holder_t* m_ep{nullptr};
    }; /* class effect_player */

} /* namespace bnb::oep */
