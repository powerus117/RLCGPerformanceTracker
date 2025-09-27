package com.gauntletperformancetracker;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializer;
import com.google.inject.Inject;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class JsonUtils
{
    @Inject
    Gson gson;

    private boolean writeFile(String filePath, String contents)
    {
        try
        {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
            writer.write(contents);
            writer.close();
            return true;
        }
        catch (IOException e)
        {
            log.error("Unable to write JSON file at path: " + filePath + "\n" + e.getMessage());
        }

        return false;
    }

    public <T, S extends JsonSerializer<T>> boolean writeJsonFile(String filePath, T data, S serializer)
    {
        String contents = gson.newBuilder()
                .registerTypeAdapter(data.getClass(), serializer)
                .setPrettyPrinting()
                .create()
                .toJson(data);

        return writeFile(filePath, contents);
    }
}
