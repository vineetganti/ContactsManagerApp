# Android Contacts Manager — End-to-End Code Walkthrough

> A beginner-friendly guide to ROOM DB, ViewModel, LiveData, Repository, Data Binding, and Threading

---

## Table of Contents

1. [Architecture at a Glance](#1-architecture-at-a-glance)
2. [ROOM Database — The Persistent Store](#2-room-database--the-persistent-store)
  - [Contacts.java — The Entity Class](#2a-contactsjava--the-entity-class)
  - [ContactDAO.java — The Data Access Object](#2b-contactsdaojava--the-data-access-object-dao)
  - [ContactDatabase.java — The Database + Singleton Pattern](#2c-contactdatabasejava--the-database--singleton-pattern)
3. [Repository.java — The Single Source of Truth](#3-repositoryjava--the-single-source-of-truth)
  - [ExecutorService & Runnable — Background Threading](#3a-executorservice--runnable--background-threading)
  - [Handler — Sending Results Back to the UI Thread](#3b-handler--sending-results-back-to-the-ui-thread)
4. [MyViewModel.java — The ViewModel](#4-myviewmodeljava--the-viewmodel)
5. [LiveData — Reactive Data That Updates the UI Automatically](#5-livedata--reactive-data-that-updates-the-ui-automatically)
6. [Data Binding — Connecting XML Layouts to Java Objects](#6-data-binding--connecting-xml-layouts-to-java-objects)
7. [The Activities — Putting It All Together](#7-the-activities--putting-it-all-together)
  - [MainActivity.java — The Contact List Screen](#7a-mainactivityjava--the-contact-list-screen)
  - [AddNewContactActivity.java — The Add Contact Screen](#7b-addnewcontactactivityjava--the-add-contact-screen)
  - [AddNewContactClickHandler.java — The Save Button Logic](#7c-addnewcontactclickhandlerjava--the-save-button-logic)
  - [MainActivityClickHandlers.java — The FAB Button](#7d-mainactivityclickhandlersjava--the-fab-button)
8. [MyAdapter.java — The RecyclerView Adapter](#8-myadapterjava--the-recyclerview-adapter)
9. [Full Data Flow Walkthrough — Adding a Contact](#9-full-data-flow-walkthrough--adding-a-contact)
10. [build.gradle — Dependencies & Build Features](#10-buildgradle--dependencies--build-features)
11. [Quick Reference — All Concepts in One Place](#11-quick-reference--all-concepts-in-one-place)

---

## 1. Architecture at a Glance

Before diving into individual files, it helps to see the big picture. Modern Android apps are built in **layers**. Each layer has one clear responsibility and talks only to the layer directly below it.

```
┌─────────────────────────────────────────────────────────────────┐
│  UI Layer                                                       │
│  Activities + XML Layouts + Data Binding                        │
│  What the user sees and interacts with                          │
├─────────────────────────────────────────────────────────────────┤
│  ViewModel Layer                                                │
│  MyViewModel — survives screen rotation                         │
│  Exposes LiveData; calls Repository methods                     │
├─────────────────────────────────────────────────────────────────┤
│  Repository Layer                                               │
│  Repository — single source of truth for data                   │
│  Decides where data comes from; threads all DB work             │
├─────────────────────────────────────────────────────────────────┤
│  Room DB Layer                                                  │
│  ContactDatabase (Singleton) + ContactDAO                       │
│  Persists data in SQLite on the device                          │
├─────────────────────────────────────────────────────────────────┤
│  Entity / Model                                                 │
│  Contacts.java — maps Java fields to DB columns                 │
│  The shape of one row in the contacts_table                     │
└─────────────────────────────────────────────────────────────────┘
```

Think of it like a restaurant: the customer (UI) talks to the waiter (ViewModel), the waiter coordinates with the kitchen (Repository), and the kitchen pulls ingredients from the pantry (Database). None of these know the internal details of the others — they just follow the menu.

| Data Flow Direction | What Happens |
|---|---|
| User Action → UI | User taps a button or swipes a row |
| UI → ViewModel | Activity calls a ViewModel method |
| ViewModel → Repository | ViewModel delegates the data operation |
| Repository → DAO | Repository calls the DAO method on a background thread |
| DAO → SQLite | ROOM executes the generated SQL statement |
| SQLite → LiveData → UI | ROOM signals LiveData; LiveData updates the Activity automatically |

---

## 2. ROOM Database — The Persistent Store

> **Concept:** ROOM is an Android library that lets you store data in a local SQLite database using simple Java classes and annotations instead of raw SQL. It checks your queries at compile time, so errors are caught before the app ever runs.

Imagine a notebook where you write down contacts. If you kept them only in your app's memory, they would vanish every time the app closed. ROOM is that notebook — it saves data permanently on the device.

ROOM has three main parts:

| ROOM Component | Role |
|---|---|
| `@Entity` | A Java class that maps to a database table (one class = one table) |
| `@Dao` | An interface defining what operations are available (insert, delete, query) |
| `@Database` | The main database class — entry point; always created as a Singleton |

---

### 2a. Contacts.java — The Entity Class

**File:** `Contacts.java`

> **Concept — Entity:** An Entity is a plain Java class that ROOM maps directly to a database table. Each field in the class becomes a column in the table. ROOM reads the annotations to understand how to create the table.

Think of it as a blueprint. A building blueprint describes the rooms and walls. An Entity class describes the columns and data types in the database table.

```java
@Entity(tableName = "contacts_table")
public class Contacts {

    @PrimaryKey(autoGenerate = true)
    private int id;               // unique ID, auto-generated (1, 2, 3 ...)

    @ColumnInfo(name = "contact_name")
    private String name;          // stored in DB column called 'contact_name'

    @ColumnInfo(name = "contact_email")
    private String email;         // stored in DB column called 'contact_email'

    public Contacts() {}          // required by ROOM (no-arg constructor)
    public Contacts(String name, String email) { ... }

    // Getters & Setters — ROOM reads/writes values through these
}
```

**Key annotations explained:**

- **`@Entity(tableName = ...)`** — Tells ROOM this class represents a table. The name in quotes becomes the actual SQL table name.
- **`@PrimaryKey(autoGenerate = true)`** — Every row needs a unique ID. ROOM generates this automatically, starting at 1 and incrementing.
- **`@ColumnInfo(name = ...)`** — Lets you give the database column a different name from the Java field. Here `name` in Java maps to `contact_name` in SQL.
- **Getters & Setters** — ROOM uses reflection to read and write field values through these, so they must be present.

---

### 2b. ContactDAO.java — The Data Access Object (DAO)

**File:** `ContactDAO.java`

> **Concept — DAO:** A Data Access Object (DAO) is an interface that declares what database operations exist. You write the method signature; ROOM auto-generates the actual SQL at compile time. You never write `INSERT` or `SELECT` statements yourself.

If the Entity is the blueprint, the DAO is the instruction manual for what you can do with the data — add a contact, remove one, or fetch all of them.

```java
@Dao
public interface ContactDAO {

    @Insert
    void insert(Contacts contact);      // ROOM generates: INSERT INTO contacts_table ...

    @Delete
    void delete(Contacts contact);      // ROOM generates: DELETE FROM ... WHERE id = ...

    @Query("SELECT * FROM contacts_table")
    LiveData<List<Contacts>> getAllContacts();  // returns a live-updating list
}
```

Notice that `getAllContacts()` returns `LiveData`. This means any screen observing this LiveData refreshes automatically whenever the database changes — with no manual polling needed.

---

### 2c. ContactDatabase.java — The Database + Singleton Pattern

**File:** `ContactDatabase.java`

> **Concept — Singleton Pattern:** A Singleton is a design pattern that ensures only **one instance** of a class ever exists. Every part of the app shares that one instance, preventing duplicated database connections and wasted memory.

Think of the office printer. There is one printer that every employee shares. You do not buy a new printer each time someone wants to print.

```java
@Database(entities = {Contacts.class}, version = 1)
public abstract class ContactDatabase extends RoomDatabase {

    public abstract ContactDAO getContactDAO();   // gives access to the DAO

    // === SINGLETON PATTERN ===
    private static ContactDatabase dbInstance;

    public static synchronized ContactDatabase getInstance(Context context) {
        if (dbInstance == null) {          // first call: create the database
            dbInstance = Room.databaseBuilder(
                context.getApplicationContext(),
                ContactDatabase.class,
                "contacts_db"
            ).fallbackToDestructiveMigration().build();
        }
        return dbInstance;    // every subsequent call: return the same instance
    }
}
```

**How the Singleton works, step by step:**

1. **First call to `getInstance()`:** `dbInstance` is `null`, so a new database is created and stored.
2. **Every call after that:** `dbInstance` already has a value, so the `if` block is skipped and the existing database is returned.
3. **The `synchronized` keyword:** If two threads call `getInstance()` simultaneously, only one creates the database — the other waits. This prevents a race condition where two databases would accidentally be created.

`fallbackToDestructiveMigration()` tells ROOM: if the database schema changes and no migration path is defined, wipe and recreate the database. Fine for learning projects; in production you would write proper migration scripts.

---

## 3. Repository.java — The Single Source of Truth

**File:** `Repository.java`

> **Concept — Repository Pattern:** A Repository is a class that sits between your ViewModel and data sources (database, network API, etc.). The ViewModel never touches the database directly — it always asks the Repository, which decides where data comes from and how to fetch it.

Why add this extra layer? Imagine your app later needs to also fetch contacts from a web API. Without a Repository, you would need to change the ViewModel and the UI. With a Repository, only the Repository changes — everything above it stays the same.

```java
public class Repository {
    private final ContactDAO contactDAO;
    ExecutorService executor;
    Handler handler;

    public Repository(Application application) {
        ContactDatabase db = ContactDatabase.getInstance(application);
        this.contactDAO = db.getContactDAO();
        executor = Executors.newSingleThreadExecutor();  // background thread pool
        handler  = new Handler(Looper.getMainLooper()); // for posting to UI thread
    }

    public void addContact(Contacts contact) {
        executor.execute(() -> contactDAO.insert(contact));
    }

    public void deleteContact(Contacts contact) {
        executor.execute(() -> contactDAO.delete(contact));
    }

    public LiveData<List<Contacts>> getAllContacts() {
        return contactDAO.getAllContacts();  // LiveData handles its own threading
    }
}
```

---

### 3a. ExecutorService & Runnable — Background Threading

> **Concept — ExecutorService:** Android's main thread handles all UI drawing and user input. If you run a slow operation (like a database write) on the main thread, the screen freezes. `ExecutorService` provides a pool of background threads where it is safe to run blocking work.

Picture a restaurant kitchen. The head chef (main thread) is always visible to diners and cannot disappear into the storeroom. A sous-chef (executor thread) handles time-consuming prep work out of sight.

```java
// Create a pool with ONE background thread
ExecutorService executor = Executors.newSingleThreadExecutor();

// Runnable = a block of code you hand off to a thread
executor.execute(new Runnable() {
    @Override
    public void run() {
        // This code runs on the background thread — safe for DB work
        contactDAO.insert(contact);
    }
});

// Modern shorthand using a lambda (does the same thing):
executor.execute(() -> contactDAO.insert(contact));
```

A **Runnable** is a Java interface with one method — `run()` — that contains the code you want to execute. You create a Runnable and hand it to the `ExecutorService`, which runs it on a background thread when one is free.

---

### 3b. Handler — Sending Results Back to the UI Thread

> **Concept — Handler:** You cannot update the UI (change text, show a toast) from a background thread — Android will crash. A `Handler` lets you post a task back to the main thread safely by adding it to that thread's message queue.

In this app the Handler is prepared but not used directly, because LiveData handles main-thread delivery automatically. It is good to understand for situations where you need manual control:

```java
Handler handler = new Handler(Looper.getMainLooper());

executor.execute(() -> {
    // Running on background thread:
    String result = doSlowWork();

    // Post a task back to the UI thread:
    handler.post(() -> {
        myTextView.setText(result);  // safe on main thread
    });
});
```

`Looper.getMainLooper()` returns the message queue of the main thread. `Handler` routes any task you post into that queue, where Android picks it up and executes it safely.

---

## 4. MyViewModel.java — The ViewModel

**File:** `MyViewModel.java`

> **Concept — ViewModel:** A ViewModel stores and manages UI-related data. Its superpower: it **survives screen rotations**. When you rotate your phone, the Activity is destroyed and recreated — but the ViewModel stays alive, so data is never lost or reloaded unnecessarily.

Without a ViewModel, rotating the screen while data is loading would restart the entire load operation. With a ViewModel, the new Activity simply re-attaches to the existing data.

```java
// AndroidViewModel gives us access to Application context — needed for the database
public class MyViewModel extends AndroidViewModel {

    private Repository myRepository;
    private LiveData<List<Contacts>> allContacts;

    // Constructor — called ONCE when ViewModel is first created
    public MyViewModel(Application application) {
        super(application);
        this.myRepository = new Repository(application);
    }

    // Returns a LiveData list — the UI observes this
    public LiveData<List<Contacts>> getAllContacts() {
        allContacts = myRepository.getAllContacts();
        return allContacts;
    }

    // Delegates add/delete to the Repository
    public void addNewContact(Contacts contact) { myRepository.addContact(contact); }
    public void deleteContact(Contacts contact) { myRepository.deleteContact(contact); }
}
```

**Why `AndroidViewModel` instead of `ViewModel`?** `AndroidViewModel` gives access to the `Application` context — a long-lived context not tied to any single Activity. The Repository constructor needs a context to initialise the database, so `AndroidViewModel` is the right choice here.

**How to get a ViewModel in an Activity** (so the same instance is reused across rotations):

```java
// ViewModelProvider finds an existing ViewModel or creates a new one
MyViewModel viewModel = new ViewModelProvider(this).get(MyViewModel.class);

// The same instance is returned after screen rotation automatically
```

---

## 5. LiveData — Reactive Data That Updates the UI Automatically

> **Concept — LiveData:** LiveData is a lifecycle-aware data holder. When the data changes, LiveData automatically notifies all active observers. When the observer's lifecycle owner (Activity) is paused or destroyed, updates stop — no crashes, no wasted work.

Think of LiveData like a news alert subscription. You subscribe once. Whenever breaking news arrives (data changes), you get a notification automatically. When you are asleep (Activity is in background), alerts pause. When you return, you see the latest state immediately.

```java
// In MainActivity.java
viewModel.getAllContacts().observe(
    this,  // 'this' Activity is the lifecycle owner
    new Observer<List<Contacts>>() {
        @Override
        public void onChanged(List<Contacts> contacts) {
            // Called every time the contacts_table changes
            contactsArrayList.clear();
            contactsArrayList.addAll(contacts);
            myAdapter.notifyDataSetChanged();  // refresh the list on screen
        }
    }
);
```

What makes this powerful is the automatic chain: ROOM writes data → ROOM signals LiveData → LiveData calls `onChanged()` on the main thread → UI refreshes. You write zero manual refresh code.

| LiveData Benefit | What Problem It Solves |
|---|---|
| Lifecycle awareness | Stops delivering updates when Activity is paused — no crashes from touching destroyed views |
| Auto UI refresh | No need to manually call `load()` or `refresh()` — data drives the UI |
| Rotation safety | Paired with ViewModel, data survives screen rotation without reloading |
| Thread safety | Always delivers on the main thread regardless of where the data was changed |

---

## 6. Data Binding — Connecting XML Layouts to Java Objects

> **Concept — Data Binding:** Data Binding lets you link UI components in XML layouts directly to data objects or click handlers in Java — eliminating repetitive `findViewById()` calls and manual `setText()` / `setOnClickListener()` wiring.

Without Data Binding, every field requires boilerplate:

```java
// Without Data Binding — tedious and error-prone
TextView nameView  = findViewById(R.id.textViewName);
TextView emailView = findViewById(R.id.textViewEmail);
nameView.setText(contact.getName());
emailView.setText(contact.getEmail());
```

With Data Binding, the XML layout does the wiring automatically:

```xml
<!-- contact_list_item.xml — wrap root in <layout> to enable Data Binding -->
<layout>
    <data>
        <variable name="contact" type="com.example.contactsmanagerapp.Contacts" />
    </data>

    <TextView
        android:text="@{contact.name}"   <!-- calls contact.getName() -->
        ... />
    <TextView
        android:text="@{contact.email}"  <!-- calls contact.getEmail() -->
        ... />
</layout>
```

In Java, you link the data object to the layout via the generated binding class:

```java
// In AddNewContactActivity.java
ActivityAddNewContactBinding binding =
    DataBindingUtil.setContentView(this, R.layout.activity_add_new_contact);

binding.setContact(contacts);       // link the Contacts object to the layout
binding.setClickHandler(handler);   // link the click handler to the layout

// Now any EditText the user fills in updates 'contacts' automatically.
// When 'contacts' changes, the TextViews update automatically.
```

Click handlers work the same way. Declare the handler in XML and Data Binding wires it up:

```xml
<!-- activity_add_new_contact.xml -->
<data>
    <variable name="clickHandler"
              type="com.example.contactsmanagerapp.AddNewContactClickHandler" />
</data>

<Button
    android:onClick="@{clickHandler::onSubmitBtnClicked}"
    android:text="Save Contact" />
```

Android calls `onSubmitBtnClicked(view)` automatically when the button is tapped. No `setOnClickListener()` code is needed anywhere.

---

## 7. The Activities — Putting It All Together

Activities are the screens of the app. This app has two.

---

### 7a. MainActivity.java — The Contact List Screen

**File:** `MainActivity.java`

MainActivity is the home screen. It shows all saved contacts in a scrollable list and allows the user to swipe left on any contact to delete it.

```java
// Key setup steps in onCreate():

// 1. Data Binding
mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
mainBinding.setClickHandler(handlers);  // links FAB button to click handler

// 2. RecyclerView — the scrollable list
RecyclerView recyclerView = mainBinding.recyclerview;
recyclerView.setLayoutManager(new LinearLayoutManager(this));

// 3. ViewModel (persists across screen rotations)
viewModel = new ViewModelProvider(this).get(MyViewModel.class);

// 4. Observe LiveData — UI automatically refreshes when the DB changes
viewModel.getAllContacts().observe(this, contacts -> {
    contactsArrayList.clear();
    contactsArrayList.addAll(contacts);
    myAdapter.notifyDataSetChanged();
});

// 5. Adapter — bridges data to the RecyclerView rows
myAdapter = new MyAdapter(contactsArrayList);
recyclerView.setAdapter(myAdapter);

// 6. Swipe-to-delete
new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, LEFT) {
    public void onSwiped(RecyclerView.ViewHolder holder, int direction) {
        Contacts c = contactsArrayList.get(holder.getAdapterPosition());
        viewModel.deleteContact(c);  // VM -> Repo -> DAO -> DB
    }
}).attachToRecyclerView(recyclerView);
```

> ⚠️ **Note:** The adapter is set up *after* the LiveData observer in this file. If the observer fires before the adapter is assigned, a `NullPointerException` would occur. In practice ROOM's query runs asynchronously and posts back after `onCreate()` finishes, so it works — but initialising the adapter before the observer would be safer.

---

### 7b. AddNewContactActivity.java — The Add Contact Screen

**File:** `AddNewContactActivity.java`

This is the form screen where the user enters a name and email. Data Binding links the form fields to a `Contacts` object, and the Save button to the click handler.

```java
contacts = new Contacts();           // empty model — fields filled by user input
binding.setContact(contacts);        // form fields write directly into this object
binding.setClickHandler(handler);    // Save button triggers the handler

// No manual getText() calls needed — Data Binding keeps 'contacts' up to date
```

---

### 7c. AddNewContactClickHandler.java — The Save Button Logic

**File:** `AddNewContactClickHandler.java`

Handles the Save button tap. Validates input, saves to the database, and navigates back to MainActivity.

```java
public void onSubmitBtnClicked(View view) {
    if (contact.getName() == null || contact.getEmail() == null) {
        Toast.makeText(context, "Fields cannot be empty", ...).show();
    } else {
        myViewModel.addNewContact(contact);  // VM -> Repo -> DAO -> DB
        Intent i = new Intent(view.getContext(), MainActivity.class);
        context.startActivity(i);            // go back to the list screen
    }
}
```

> 💡 **Improvement to consider:** The `null` check does not catch empty strings. A user who types a single space would pass the check. Replacing the check with `contact.getName().trim().isEmpty()` would be more robust.

---

### 7d. MainActivityClickHandlers.java — The FAB Button

**File:** `MainActivityClickHandlers.java`

A simple handler for the Floating Action Button (FAB) that opens `AddNewContactActivity` when tapped.

```java
public void onFABClicked(View view) {
    Intent i = new Intent(view.getContext(), AddNewContactActivity.class);
    context.startActivity(i);
}
```

---

## 8. MyAdapter.java — The RecyclerView Adapter

**File:** `MyAdapter.java`

> **Concept — RecyclerView Adapter:** A RecyclerView displays a long scrollable list efficiently. Its Adapter is the bridge between your data (list of Contacts) and the visible rows on screen. RecyclerView **reuses** row views as you scroll to save memory.

Three methods you must implement:

| Adapter Method | When It Is Called & What It Does |
|---|---|
| `onCreateViewHolder()` | Called when RecyclerView needs a brand-new row view. Inflates the XML layout and returns a ViewHolder. |
| `onBindViewHolder()` | Called each time a row is about to appear on screen. Binds the correct Contact data to that row's views. |
| `getItemCount()` | Called to find out how many rows the list has. Returns the size of the contacts list. |

```java
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ContactViewHolder> {

    // 1. Create a new row (called when list needs a row not yet in memory)
    public ContactViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ContactListItemBinding binding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.getContext()),
            R.layout.contact_list_item, parent, false
        );
        return new ContactViewHolder(binding);
    }

    // 2. Bind data to an existing row as it scrolls into view
    public void onBindViewHolder(ContactViewHolder holder, int position) {
        Contacts current = contacts.get(position);
        holder.binding.setContact(current);  // Data Binding fills the TextViews
    }

    // 3. How many rows?
    public int getItemCount() {
        return contacts != null ? contacts.size() : 0;
    }

    // ViewHolder — caches view references so we don't look them up repeatedly
    class ContactViewHolder extends RecyclerView.ViewHolder {
        ContactListItemBinding binding;
        ContactViewHolder(ContactListItemBinding b) {
            super(b.getRoot());
            this.binding = b;
        }
    }
}
```

The **ViewHolder pattern** is a performance optimisation. Without it, `onBindViewHolder()` would call `findViewById()` on every scroll event, which is expensive. ViewHolder stores the view references once when the row is created and reuses them.

---

## 9. Full Data Flow Walkthrough — Adding a Contact

Here is exactly what happens, step by step, when the user adds a new contact:

1. User fills in the name and email fields in the form (`activity_add_new_contact.xml`).
2. Data Binding writes those values into the `contacts` object automatically via two-way binding.
3. User taps Save. Data Binding calls `onSubmitBtnClicked()` in `AddNewContactClickHandler`.
4. Click handler calls `myViewModel.addNewContact(contact)`.
5. ViewModel calls `myRepository.addContact(contact)`.
6. Repository submits the insert to the `ExecutorService` on a background thread.
7. Background thread calls `contactDAO.insert(contact)`. ROOM generates and runs the SQL `INSERT`.
8. ROOM detects that `contacts_table` has changed and notifies the LiveData.
9. LiveData delivers the updated list to `onChanged()` in MainActivity on the main thread.
10. The adapter clears the old list, adds the new data, and calls `notifyDataSetChanged()`.
11. The new contact appears in the RecyclerView — without a single manual refresh call.

---

## 10. build.gradle — Dependencies & Build Features

The `build.gradle` file declares which libraries are compiled into the app and which Android build features are enabled:

```kotlin
buildFeatures {
    viewBinding = true
    dataBinding = true    // enables the @{...} expression syntax in XML
}

dependencies {

    // ROOM — local SQLite database with annotation processing
    implementation("androidx.room:room-runtime:2.8.4")
    annotationProcessor("androidx.room:room-compiler:2.8.4")
    // room-compiler reads your @Entity, @Dao, @Database annotations at
    // compile time and generates the actual SQL implementation code for you

    // ViewModel — persists UI data across screen rotations
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")

    // LiveData — reactive, lifecycle-aware data observations
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.10.0")
}
```

The `annotationProcessor` for `room-compiler` is critical: it reads all ROOM annotations at compile time and generates the concrete Java classes that actually execute your SQL queries. Without it, ROOM would not know how to create a working database from your annotations.

---

## 11. Quick Reference — All Concepts in One Place

| Concept | One-Line Summary |
|---|---|
| **ROOM Database** | Android library for persistent local storage using SQLite, with compile-time SQL verification |
| **@Entity** | Java class that maps 1-to-1 with a database table; each field is a column |
| **@Dao** | Interface declaring database operations; ROOM generates the SQL implementation for you |
| **@Database** | Abstract class that is the entry point to the ROOM database; always used as a Singleton |
| **Singleton Pattern** | Design pattern ensuring only one instance of a class exists; shared across the whole app |
| **Repository Pattern** | Class that is the single point of contact for data; decouples ViewModel from data sources |
| **ExecutorService** | Manages a pool of background threads for running slow operations off the main thread |
| **Runnable** | A block of code (one `run()` method) that you hand to a thread to execute |
| **Handler** | Routes tasks back to the main (UI) thread safely from a background thread |
| **ViewModel** | Stores UI data and survives screen rotations; never holds a reference to an Activity |
| **AndroidViewModel** | ViewModel subclass that also has Application context; use when a context is needed |
| **LiveData** | Lifecycle-aware observable data holder; auto-updates UI and stops when Activity is gone |
| **Data Binding** | Connects XML layout variables to Java objects and methods, removing boilerplate view code |
| **RecyclerView Adapter** | Bridge between a list of data and the scrollable rows displayed on screen |
| **ViewHolder** | Caches view references in the Adapter to avoid expensive repeated lookups while scrolling |

---

*Every expert was once a beginner who refused to stop asking why. Happy coding!*