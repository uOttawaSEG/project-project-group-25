package com.example.deliverable_1_seg;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.deliverable_1_seg.helpers.db.Event;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FirebaseEventHelper {
    private static final String TAG = "FirebaseEventHelper";
    public final DatabaseReference eventsRef;

    public FirebaseEventHelper() {
        // Initialize Firebase reference for events
        eventsRef = FirebaseDatabase.getInstance().getReference("events");
    }

    public interface writeCallback {
        void onSuccess();
        void onFailure(DatabaseError error);
    }

    public interface DataStatus {
        void DataLoaded(List<Event> events);
        void onError(DatabaseError error);
    }

    //method called when creating event
    public void addEvent(Event event, writeCallback callback){
        String eventID = eventsRef.push().getKey();

       if (eventID != null){
           event.setEventId(eventID);
           eventsRef.child(eventID).setValue(event).addOnCompleteListener(task -> {
               if (task.isSuccessful()){
                   Log.d(TAG, "Event added successfully");
                   callback.onSuccess();
               } else {
                   Log.e(TAG, "Failed to add event", task.getException());
                   callback.onFailure(DatabaseError.fromException(task.getException()));
               }
           });
       } else {
           Log.e(TAG, "Failed to generate unique event ID.");
           callback.onFailure(DatabaseError.fromException(new Exception("Failed to generate unique event ID.")));
       }
    }

    //loads the organizers current events
    public void loadEventsForCurrentUser (String organizerId, DataStatus dataStatus){
        eventsRef.orderByChild("organizerId").equalTo(organizerId).addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot){
                List<Event> eventsList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Event event = snapshot.getValue(Event.class);
                    if (event != null){
                        event.setEventId(snapshot.getKey());

                        // Ensure people and requests lists are not null
                        if (event.getPeople() == null) {
                            event.setPeople(new HashMap<>());
                        }
                        if (event.getRequests() == null) {
                            event.setRequests(new HashMap<>());
                        }

                        eventsList.add(event);
                    }
                }
                dataStatus.DataLoaded(eventsList);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load events", databaseError.toException());
                dataStatus.onError(databaseError);
            }
        });
    }

    //loads the all current events
    public void loadAllEvents (DataStatus dataStatus){
        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(DataSnapshot dataSnapshot){
                List<Event> eventsList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Event event = snapshot.getValue(Event.class);
                    if (event != null){
                        if (event.getPeople() == null) {
                            event.setPeople(new HashMap<>());
                        }
                        if (event.getRequests() == null) {
                            event.setRequests(new HashMap<>());
                        }
                        event.setEventId(snapshot.getKey());
                        eventsList.add(event);
                    }
                }
                dataStatus.DataLoaded(eventsList);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load events", databaseError.toException());
                dataStatus.onError(databaseError);
            }
        });
    }

    public void deleteEvent(String eventId, writeCallback callback){
        if (eventId != null){
            eventsRef.child(eventId).removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Event deleted successfully");
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to delete event", task.getException());
                    callback.onFailure(DatabaseError.fromException(task.getException()));
                }
            });
        } else {
            Log.e(TAG, "Event ID is null. Cannot delete event.");
        }
            callback.onFailure(DatabaseError.fromException(new Exception("Event ID is null")));
    }

    public void joinEvent(String eventId, String userID, writeCallback callback){
        if (eventId != null && userID != null) {
            DatabaseReference peopleRef = eventsRef.child(eventId).child("people");

            // Add the user ID to the "people" list (push creates a new entry userID as key, second value as true)
            peopleRef.child(userID).setValue(true).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "User joined event successfully");
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to join event", task.getException());
                    callback.onFailure(DatabaseError.fromException(task.getException()));
                }
            });
        } else {
            Log.e(TAG, "Event ID or User ID is null. Cannot join event.");
            callback.onFailure(DatabaseError.fromException(new Exception("Event ID or User ID is null")));
        }
    }

    public void requestEvent(String eventId, String userID, writeCallback callback){
        if (eventId != null && userID != null) {
            DatabaseReference requestsRef = eventsRef.child(eventId).child("requests");

            // Add the user ID to the "requests" list
            requestsRef.child(userID).setValue(true).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "User requested event successfully");
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to request event", task.getException());
                    callback.onFailure(DatabaseError.fromException(task.getException()));
                }
            });
        } else {
            Log.e(TAG, "Event ID or User ID is null. Cannot request event.");
            callback.onFailure(DatabaseError.fromException(new Exception("Event ID or User ID is null")));
        }
    }

    public void removeUserFromPeople(String eventId, String userID, writeCallback callback) {
        if (eventId != null && userID != null) {
            DatabaseReference peopleRef = eventsRef.child(eventId).child("people").child(userID);

            // Remove the user ID key
            peopleRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "User removed from people list successfully");
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to remove user", task.getException());
                    callback.onFailure(DatabaseError.fromException(task.getException()));
                }
            });
        } else {
            Log.e(TAG, "Event ID or User ID is null. Cannot remove user from people list.");
            callback.onFailure(DatabaseError.fromException(new Exception("Event ID or User ID is null")));
        }
    }


    public void removeUserFromRequests(String eventId, String userID, writeCallback callback) {
        if (eventId != null && userID != null) {
            DatabaseReference requestsRef = eventsRef.child(eventId).child("requests").child(userID);

            // Remove the user ID key directly
            requestsRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "User removed from requests list successfully");
                    callback.onSuccess();
                } else {
                    Log.e(TAG, "Failed to remove user from requests list", task.getException());
                    callback.onFailure(DatabaseError.fromException(task.getException()));
                }
            });
        } else {
            Log.e(TAG, "Event ID or User ID is null. Cannot remove user from requests list.");
            callback.onFailure(DatabaseError.fromException(new Exception("Event ID or User ID is null")));
        }
    }


}
