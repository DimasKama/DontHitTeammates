package com.dimaskama.donthitteammates.client.config;

import java.util.ArrayList;
import java.util.List;

public class DHTConfig extends Config {
    public boolean save_enabled_state = true;
    public boolean enabled = true;
    public boolean save_teammates_list = true;
    public List<Teammate> teammates = new ArrayList<>();

    public DHTConfig(String path) {
        super(path);
    }
}
