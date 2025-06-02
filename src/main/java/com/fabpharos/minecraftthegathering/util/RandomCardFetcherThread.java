package com.fabpharos.minecraftthegathering.util;

import com.fabpharos.minecraftthegathering.item.CardItem;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.item.ItemStack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class RandomCardFetcherThread extends Thread{
    private static final URL url;
    private final ItemStack stack;

    static {
        try {
            url = new URL("https://api.scryfall.com/cards/random?q=(-type%3Abasic+-type%3Atoken+-is%3Aback+-is%3Adigital+-is%3Afunny)+is%3Abooster");
        } catch (MalformedURLException e) {
            throw new RuntimeException();
        }
    }

    public RandomCardFetcherThread(ItemStack stack) {
        super();
        this.stack = stack;
    }
    @Override
    public void run() {
        HttpURLConnection con;
        if(stack == null || !(stack.getItem() instanceof CardItem))
            return;
        try {
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setRequestProperty("Content-Type", "application/json");
            int status = con.getResponseCode();
            BufferedReader streamReader = null;
            if(status > 299) {
                return;
            } else {
                streamReader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            }
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = streamReader.readLine()) != null) {
                content.append(inputLine);
            }
            streamReader.close();
            con.disconnect();
            JsonObject json = (JsonObject) JsonParser.parseString(content.toString());
            MagicCardUtils.assignCard(stack, json, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
