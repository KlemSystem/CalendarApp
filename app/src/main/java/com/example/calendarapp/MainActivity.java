
package com.example.calendarapp;

import android.app.AlertDialog;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private Map<String, List<String>> eventsMap = new HashMap<>();
    private String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the locale to Russian
        Locale locale = new Locale("ru", "RU");
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        CalendarView calendarView = findViewById(R.id.calendarView);
        ImageView btnAddEvent = findViewById(R.id.btnAddEvent);
        ListView eventList = findViewById(R.id.eventList);
        TextView noEventsText = findViewById(R.id.noEventsText);
        TextView selectedDateText = findViewById(R.id.selectedDateText);

        loadEventsFromFile();

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            selectedDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(calendar.getTime());
            selectedDateText.setText(selectedDate + ":");
            showEvents(eventList, noEventsText);
        });

        btnAddEvent.setOnClickListener(v -> showAddEventDialog(eventList, noEventsText));

        selectedDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        selectedDateText.setText(selectedDate + ":");
        showEvents(eventList, noEventsText);
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveEventsToFile();
    }

    private void showEvents(ListView eventList, TextView noEventsText) {
        List<String> events = eventsMap.getOrDefault(selectedDate, new ArrayList<>());
        if (events.isEmpty()) {
            noEventsText.setVisibility(View.VISIBLE);
            eventList.setVisibility(View.GONE);
        } else {
            noEventsText.setVisibility(View.GONE);
            eventList.setVisibility(View.VISIBLE);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, events);
            eventList.setAdapter(adapter);

            eventList.setOnItemClickListener((parent, view, position, id) ->
                    showEditOrDeleteDialog(events.get(position), eventList, noEventsText));
        }
    }

    private void showAddEventDialog(ListView eventList, TextView noEventsText) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_event, null);
        EditText eventNameInput = dialogView.findViewById(R.id.eventNameInput);

        new AlertDialog.Builder(this)
                .setTitle("Добавить мероприятие")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    String eventName = eventNameInput.getText().toString();
                    if (!eventName.isEmpty()) {
                        List<String> events = eventsMap.computeIfAbsent(selectedDate, k -> new ArrayList<>());
                        events.add(eventName);
                        showEvents(eventList, noEventsText);
                    } else {
                        Toast.makeText(this, "Название мероприятия не может быть пустым", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .create()
                .show();
    }

    private void showEditOrDeleteDialog(String event, ListView eventList, TextView noEventsText) {
        new AlertDialog.Builder(this)
                .setTitle("Выберите действие")
                .setItems(new String[]{"Редактировать", "Удалить"}, (dialog, which) -> {
                    if (which == 0) {
                        showEditEventDialog(event, eventList, noEventsText);
                    } else if (which == 1) {
                        deleteEvent(event, eventList, noEventsText);
                    }
                })
                .create()
                .show();
    }

    private void showEditEventDialog(String event, ListView eventList, TextView noEventsText) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_event, null);
        EditText eventNameInput = dialogView.findViewById(R.id.eventNameInput);
        eventNameInput.setText(event);

        new AlertDialog.Builder(this)
                .setTitle("Редактировать мероприятие")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    String newEventName = eventNameInput.getText().toString();
                    if (!newEventName.isEmpty()) {
                        List<String> events = eventsMap.get(selectedDate);
                        if (events != null) {
                            events.remove(event);
                            events.add(newEventName);
                        }
                        showEvents(eventList, noEventsText);
                    } else {
                        Toast.makeText(this, "Название мероприятия не может быть пустым", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .create()
                .show();
    }

    private void deleteEvent(String event, ListView eventList, TextView noEventsText) {
        List<String> events = eventsMap.get(selectedDate);
        if (events != null) {
            events.remove(event);
            showEvents(eventList, noEventsText);
        }
    }

    private void saveEventsToFile() {
        try {
            FileOutputStream fos = openFileOutput("events.json", MODE_PRIVATE);
            Gson gson = new Gson();
            String json = gson.toJson(eventsMap);
            fos.write(json.getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка сохранения данных", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadEventsFromFile() {
        try {
            FileInputStream fis = openFileInput("events.json");
            InputStreamReader isr = new InputStreamReader(fis);
            Gson gson = new Gson();
            eventsMap = gson.fromJson(isr, new TypeToken<Map<String, List<String>>>() {}.getType());
            if (eventsMap == null) {
                eventsMap = new HashMap<>();
            }
            isr.close();
        } catch (FileNotFoundException e) {
            eventsMap = new HashMap<>();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
        }
    }
}