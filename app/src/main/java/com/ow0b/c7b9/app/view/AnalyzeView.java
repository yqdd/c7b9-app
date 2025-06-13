package com.ow0b.c7b9.app.view;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.util.ApiCallback;
import com.ow0b.c7b9.app.util.ApiClient;
import com.ow0b.c7b9.app.util.Toast;
import com.ow0b.c7b9.app.util.midi.Midi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

public class AnalyzeView extends LinearLayout
{
    public HashSet<RecordBackground> backgrounds = new HashSet<>();
    private final BarChartView speed;
    private final ViewGroup speedView;
    private final LineChartView force;
    private final ViewGroup forceView;
    private final PianoRollView standMidi;
    private final ViewGroup standMidiView;
    private final PianoRollView userMidi;
    private final ViewGroup userMidiView;

    public AnalyzeView(Context context)
    {
        super(context);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        setOrientation(VERTICAL);

        speed = new BarChartView(context);
        force = new LineChartView(context);
        standMidi = new PianoRollView(context);
        userMidi = new PianoRollView(context) {{ setColorRed(true); }};

        addView(userMidiView = PianoRollView.getView(context, userMidi));
        addView(standMidiView = PianoRollView.getView(context, "", standMidi));
        addView(forceView = LineChartView.getView(context, force));
        addView(speedView = BarChartView.getView(context, speed));
        userMidiView.setVisibility(View.GONE);
        standMidiView.setVisibility(View.GONE);
        forceView.setVisibility(View.GONE);
        speedView.setVisibility(View.GONE);

        backgrounds.add(userMidi);
        backgrounds.add(force);
    }

    public void compileJsonObject(Activity activity, JsonObject json)
    {
        //设置速度
        JsonArray speedJson = jsonNullGet(json, "speed", JsonElement::getAsJsonArray);
        if(speedJson != null) setSpeed(toList(speedJson, ele -> (int) (ele.getAsFloat() * 100)));

        //设置匹配到的midi
        String match = jsonNullGet(json, "match", JsonElement::getAsString);
        JsonArray indexesJson = jsonNullGet(json, "indexes", JsonElement::getAsJsonArray);
        if(match != null)
        {
            try(FileInputStream stream = new FileInputStream(midiFile(activity, match)))
            {
                setStandMidi(getCachedMidi(activity, match, indexesJson));
            }
            catch (IOException e)
            {
                downloadLibraryMidi(activity, match, () -> activity.runOnUiThread(() ->
                {
                    setStandMidi(getCachedMidi(activity, match, indexesJson));
                }));
            }
        }

        //设置用户演奏的midi
        Midi midi = jsonNullGet(json, "midi", ele -> new Gson().fromJson(ele.getAsJsonObject(), Midi.class));
        if(midi != null) setUserMidi(midi);

        //设置力度
        JsonArray f1Json = jsonNullGet(json, "force1", JsonElement::getAsJsonArray),
                f2Json = jsonNullGet(json, "force2", JsonElement::getAsJsonArray);
        Integer phase = jsonNullGet(json, "forcePhase", JsonElement::getAsInt),
                tail = jsonNullGet(json, "forceTail", JsonElement::getAsInt);
        if(f1Json != null && f2Json != null)
        {
            setForce(toList(f1Json, ele -> ele.getAsFloat() * 100),
                    toList(f2Json, ele -> ele.getAsFloat() * 100),
                    phase == null ? 0 : phase, tail == null ? 0 : tail);
        }
    }

    private Midi getCachedMidi(Activity activity, String name, JsonArray indexesJson)
    {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(midiFile(activity, name)))))
        {
            StringBuilder sb = new StringBuilder();
            String str;
            while((str = reader.readLine()) != null) sb.append(str);
            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
            Midi midi = new Gson().fromJson(json.get("data").getAsJsonObject(), Midi.class);
            if(indexesJson != null)
            {
                List<Integer> indexes = new ArrayList<>();
                indexesJson.forEach(ele -> indexes.add(ele.getAsInt()));
                midi.indexes = indexes;
            }
            return midi;
        }
        catch (IOException e)
        {
            Log.e("TAG", "onResponse: ", e);
            Toast.showError(activity, "读取Midi数据时出现未知错误");
            return new Midi();
        }
    }
    private void downloadLibraryMidi(Activity activity, String name, Runnable callback)
    {
        ApiClient.getInstance(activity).url(activity.getResources().getString(R.string.server) + "api/library")
                .parameter("name", name)
                .get()
                .callback(new ApiCallback(activity)
                {
                    @Override
                    public void onResponse(@NonNull String response)
                    {
                        try(Writer writer = new OutputStreamWriter(new FileOutputStream(midiFile(activity, name))))
                        {
                            writer.write(response);
                            callback.run();
                        }
                        catch (IOException e)
                        {
                            Toast.showError(activity, "下载Midi数据出现未知错误");
                        }
                    }
                })
                .enqueue();
    }
    private static File midiFile(Context context, String name)
    {
        return context.getCacheDir().toPath().resolve(name).toFile();
    }

    public <T> T jsonNullGet(JsonObject json, String attr, Function<JsonElement, T> getter)
    {
        return json.get(attr) != null ? getter.apply(json.get(attr)) : null;
    }
    private <T> List<T> toList(JsonArray array, Function<JsonElement, T> getter)
    {
        List<T> list = new ArrayList<>();
        array.forEach(ele -> list.add(getter.apply(ele)));
        return list;
    }

    public void setSpeed(List<Integer> data)
    {
        speed.setData(data);
        speedView.setVisibility(VISIBLE);
    }
    public void setForce(List<Float> force1, List<Float> force2, int forcePhase, int forceTail)
    {
        force.setData(force1, force2, forcePhase, forceTail);
        forceView.setVisibility(VISIBLE);
    }
    public void setStandMidi(Midi midi)
    {
        standMidi.setMidi(midi);
        if(standMidiView.getChildAt(0) instanceof TextView textView) textView.setText(midi.name);
        standMidiView.setVisibility(VISIBLE);
    }
    public void setUserMidi(Midi midi)
    {
        userMidi.setMidi(midi);
        userMidiView.setVisibility(VISIBLE);
    }
}
