Adding in dependencies from androidx.room, implementation and annotation processor
Annotation processor - Compile time check and for operations alike insert, delete, update. 
Automate repetitive code generation and to enforce specific behaviours in codebase.

### Contacts.java
Annotating @Entity
Entity is a fundamental component that represents a table in the SQLite database.
Each entity class corresponds to one table and the fields properties or the variables within the entity
class represent INT columns in that table.

Entities define the structure and the schema of your database tables.
As mentioned before, we have only one entity class called contacts, and three
columns, contact ID, contact name and contact email one of type int and two of type strings.

Can specify various attributes within the entity annotation such as the table name, primary keys and indices.


@ColumnInfo
Used to specify additional details about how a field, property or variable in an entity class maps to a
column in the database table.

Provides control over column specific properties and behaviors, explicitly specify the 
name of the database column that corresponds to a field in the entity.
Eg - @ColumnInfo
private int id;
private String name;
private String email;
}
Using @ColumnInfo annotation, we can specify our own names for the columns, 
apart from specifying the java variables we declare in the class

@ColumnInfo(name = "contact_id")
@PrimaryKey(autoGenerate = true)
private int id;

@ColumnInfo(name = "contact_name")
private String name;

@ColumnInfo(name = "contact_email")
private String email;

### ContactDAO.java
@Dao
Specifies a contract for interacting with the database, including methods for inserting, updating
deleting and querying data using annotations like @Insert, @Delete, @Query
Eg-
@Dao
public interface ContactDAO {
@Insert
void insert(Contacts contact);

@Delete
void delete(Contacts contact);

@Query("SELECT * FROM contacts_table")
List<Contacts> getAllContacts();
}

### ContactDatabase.java
- Database class is an abstract class that serves as the database holder and includes methods to access
DAOs and create a database instance
- Use @Database annotation to specify list of entities and database version
- Eg- @Database(entities = {Contacts.class}, version = 1)


- Create the class as an abstract to prevent any possible creation of this class. ContactDatabase will 
be our database instance which will extend RoomDatabase.
- public  abstract class ContactDatabase extends RoomDatabase {
}
- It's an abstract class and annotated with database and we specify the entity, linking the database with the entity
- To link the database with the DAO, public abstract ContactDAO contactDAO();
- Following singleton pattern to provide one instance during life cycle of the app. 
Singleton ensures only one instance of the database exists throughout application lifecycle, optimizing resource usage. 
-- private static ContactDatabase dbInstance;
- public static synchronized ContactDatabase getInstance(Context context) {} - getInstance() is 
used to provide a contact database when it's cold. Check any existing instance of the database during lifecycle 
of the app else create a new instance of the ROOM database.
- dbInstance = Room.databaseBuilder(
  context.getApplicationContext(),
  ContactDatabase.class,
  "contacts_db");
- Room.databaseBuilder is a factory method provided by the room library 
to create a new database or access an existing one
- context.getApplicationContext() - Typically the context of the application like an activity or application
and the getApplicationContext method is often preferred when working with databases because of it's longer
lifecycle than activity context.
- ContactDatabase.class -- The rooms database and "contacts_db" is the name of the database. They are file based.
- .fallbackToDestructiveMigration() -- This is passed to check if a new version of database schema is detected due to changes
in the entity structure. If detected, room will drop and recreate the database effectively 
discarding all existing data. Useful during development or when it is acceptable to lose existing data for
production apps.
- the .build() method is called to build the room database instance according to the specified configuration.
- Else if there is any instance created in the previous executions or commands, then I need to return dbInstance.


### Repository

- Class mainly used to manage multiple sources of data
- Isolates the data sources from the rest of the app and provides a clean API for data access to rest of the app.
- Can gather data from different REST APIs, cache, local database storage.
- For this project, a repository is not needed but we will create one for demonstration purposes.
- private final ContactDAO contactDAO; -- When dealing with room databases, deal with the ContactDAO object in the repository.
- This repository encapsulates data access and retrieval logic. It receives a contact Dao object 
in its constructor, which provides access to the local database, assuming you are using room.
- So we have three methods insert, delete and get all contacts we need to use these methods from the repository.
- Add contact.
It receives a contact object we need to call this method insert in the Dao.
So we use this contact dao object dot insert.
And here we need to pass this parameter as a parameter inside this insert method.
It's just managing all the methods in the different data sources.
Again, if we have only one data source, we don't need to use the repository.
  public void addContact(Contacts contact) {
  contactDAO.insert(contact);
  }
- Publicvoid delete contact receives a contact object and execute the contact Dao dot delete method and 
passing the contact as the argument for this delete method.
- If we have any source of data, we need to mention all the methods here in the repository.


- Room database operations such as insertions, updates and queries should not be executed on the main
UI thread because they can potentially block the UI causing the app to become unresponsive.
We need to offload these database operations to background threads.
By doing this, you keep the UI thread free to handle user interactions and ensure that your app remains
responsive.
- To prevent any possible errors and UI crashes, we need to use handlers and the executer services.
- Usually a thread pool executor is used to offload these database operations to background threads.
By doing this, we keep the UI thread free to handle user interactions and ensure that our app remains
responsive as single threaded executer is created, meaning that database operation will be executed
sequentially in a background thread.
-  UI updates must be performed on the main UI thread to avoid view related issues. The handler with 
Looper dot get main Looper method is used to post tasks to the main UI threads messages queue. Ensures
that any UI related code such as updating text views or recycler views is executed on the main thread.
   Handler handler = new Handler(Looper.getMainLooper());
- You achieve many benefits such as thread separation synchronization and responsive UI thread separation.

### Executing Tasks on a Separate Thread Using Runnable
  The Core Problem
  When performing operations like database reads/writes, running them on the main (UI) thread causes
  the app to freeze or crash. So these tasks must be offloaded to a background thread.
  The Key Components
1. Runnable
   An interface with a single method — void run() — that wraps the code you want to execute asynchronously.
Think of it as a "task container."
2. ExecutorService
   Manages a pool of background threads. You submit a Runnable to it, and it handles running that
task off the main thread.
3. Handler
   Lives on the main (UI) thread. Once the background task finishes, the Handler is used to safely push
UI updates back to the main thread.


### LiveData

- Live data is typically used to expose data from the room database to the ViewModel and ultimately to
the UI components, activities or fragments.

- The repository interacts with the room database to retrieve data and it wraps the data in a live data
object before passing it to the ViewModel.

- By using the live data, the repository ensures that the data can be observed by the ViewModel, and
any changes in the underlying data are automatically reflected in the UI without the need to for explicit
updates.

### ViewModel
- The purpose of the ViewModel is to acquire and keep the information that is necessary for an activity
or a fragment
- Acts as a link between model and View. Responsible for transforming data from the model.
- Provides data streams to the view.
- If you need to use the context inside your view model, you should use Android View model AVM instead
of a view model because it contains the application context.
- By adding in methods from the Repository.java for fetching all contacts, add contact and delete contact,
we can provide data to the UI, observe changes in the user data and automatically update UI when data changes
and contacts can be inserted into room database through ViewModel and also be deleted.
- The usage of MVVM architecture is clearly displayed here with separation of ViewModel with UI and data source.

### Data binding with RecyclerView
### MyAdapter.java - Line by line Breakdown
#### Class declaration & Setup

``` java 
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ContactViewHolder>
```

MyAdapter extends RecyclerView.Adapter, which is the base class for all recycler view adapters. The generic
type <MyAdapter.ContactViewHolder> tells the adapter which ViewHolder class it will use.
In this case, the inner class, ContactViewHolder defined at the bottom

---

```java 
private ArrayList<Contacts> contacts;
```

Data source - a list of Contacts objects. The adapter reads from this list to 
populate each row in the RecyclerView

---

```java
public MyAdapter(ArrayList<Contacts> contacts) {
this.contacts = contacts;
}
```

The constructor receives the list from the outside, from the Activity or the Fragment and stores it 
locally. this.contacts refers to the field above; contacts (without this) is the parameter passed in.

---

#### onCreateViewHolder() - Inflating the Layout
```java 
public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType);
```

RecyclerView calls this when it needs a new row view - either at startup or when a new item scrolls into view. 
It should inflate the item layout and return a ViewHolder wrapping it.

---

```java
ContactListItemBinding contactListItemBinding = 
DataBindingUtil.inflate(
    LayoutInflater.from(parent.getContext()), //1
    R.layout.contact_list_item, //2
    parent, //3
        false //4
);
```

Instead of a plain View, data binding give a binding object that already has references to all views
inside the layout

The four parameters numbered above,
1. LayoutInflater.from(parent.getContext()) - Creates an inflater using the parent's context needed 
to read and build XML layouts
2. R.layout.contact_list_item - Points to the XML layout file for a single row
3. parent - The RecyclerView itself - lets the inflater calculate correct layout params
4. false - Do not attach immediately; the RecyclerView will do

---

```java
return new ContactViewHolder(contactListItemBinding);
```
Wraps the binding object in a ContactViewHolder and returns it. RecyclerView will recycle and reuse
this folder for multiple rows.

---

#### onBindViewHolder() - Populating data into a row

```java
public void onBindViewHolder(@NonNull ContactViewHolder holder, int position);
```
Called everytime a row becomes visible while scrolling. holder is a recycled (or newly created) ViewHolder;
position is the index in the list.

---

```java
Contacts currentContact = contacts.get(position);
```

Fetches the Contacts object at the current scroll position from the ArrayList.

---

```java
holder.contactListItemBinding.setContact(currentContact);
```

This is the data binding magic: Instead of manually doing textView.settext(currentContact.getName()) 
for every field, we pass the entire object to the layout. The XML layout's <variable> tag of type Contacts
receives it, and binding expressions like @{contact.name} update automatically.

---

#### getItemCount - Telling RecyclerView the list size
```java
public int getItemCount() {
    if (contacts != null) {
        return contacts.size();
    } else {
        return 0;
    }
}
```
RecyclerView calls this to know how many rows to render. The null check prevents a crash if the adapter
is set up before data has loaded.

---

#### setContacts - Updating the data source
```java
public void setContacts(ArrayList<Contacts> contacts) {
    this.contacts = contacts;
    notifyDataSetChanged();
}
```

When new data arrives from a database, we call this method. notifyDataSetChanged() tells the RecyclerView
"the data changed, re-draw everything". Simplest but least efficient way to update the data.
Fine for small lists. 

---

#### ContactViewHolder - Inner Class
```java
class ContactViewHolder extends RecyclerView.ViewHolder {
    private ContactListItemBinding contactListItemBinding;
```

The ViewHolder's job is to hold a reference to the binding object so onBindViewHolder can reach the views
without calling findViewById repeatedly (which is slow).

---

```java
public ContactViewHolder(@NonNull ContactListItemBinding contactListItemBinding) {
    super(contactListItemBinding.getRoot());
    this.contactListItemBinding = contactListItemBinding;
}
```

Two things happen here:

- super(contactListItemBinding.getRoot()) - RecyclerView.ViewHolder's constructor requires the root
View of the item layout. .getRoot() extracts that top-level View from the binding object.
- this.contactListItemBinding = contactListItemBinding - stores the binding so onBindViewHolder can call 
.setContact() on it later.

#### The overall flow

Activity creates MyAdapter(contactsList)

RecyclerView calls onCreateViewHolder → inflates XML → returns ViewHolder

RecyclerView calls onBindViewHolder → gets Contact at position → binding.setContact()

Data binding expressions in XML automatically update the TextViews

User scrolls → ViewHolder is recycled → onBindViewHolder is called again with new position

Key advantage of data binding here is that onBindViewHolder stays a single line - no manual seText
calls needed for each field.


### Getting data into ROOM DB and Displaying contacts into RecyclerView

```java
private ContactDatabase contactDatabase;
```
Declares a reference to the Room database instance. The singleton pattern ensures only one instance exists
throughout the app

```java
private ArrayList<Contacts> contactsArrayList = new ArrayList<>();
```
Creates an empty ArrayList to hold contact objects fetched from the database. Initialized immediately
to avoid the NullPointerException the transcript describes encountering.

```java
private MyAdapter myAdapter;
```
Declares the RecyclerView adapter that will bridge the data and the UI.

```java
private ActivityMainBinding mainBinding;
```
The data binding object auto-generated from activity_main.xml. Used to access UI elements 
without findViewById.

```java
private MainActivityClickHandlers handlers;
```
Declares the click handler class that manages Floating Action Button (FAB) events.

---

#### Inside OnCreate

```java
mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
```
Sets the content view using Android's Data Binding library instead of the standard setContentView.
Returns a binding object tied to activity_main.xml.

```java
handlers = new MainActivityClickHandlers(this);
mainBinding.setClickHandler(handlers);
```
Creates the click handler instance and links it to the layout binding so XML can reference
click events directly.

```java
RecyclerView recyclerView = mainBinding.recyclerview;
recyclerView.setLayoutmanager(new LinearLayoutManager(this));
recyclerView.setHasFixedSize(true);
```
Retrieves the RecyclerView from the binding object (instead of findViewById), assigns a vertical 
LinearLayoutManager, and tells it the size won't change for performance optimization.

```java
myAdapter = new MyAdapter(contactsArrayList);
```

Creates the adapter, passing the empty ArrayList. The adapter will display whatever is in this list.

```java
contactDatabase = ContactDatabase.getInstance(this);
```
Gets the single shared instance of the ROOM database using the singleton pattern
defined in ContactDatabase.

```java
MyViewModel viewModel = new ViewModelProvider(this).get(MyViewModel.class);
```
Creates or retrieves an existing ViewModel scoped to this activity. The ViewModel survives configuration
changes like screen rotation. It sits between the UI and the repository/ database.

```java
Contacts c1 = new Contacts("Jack", "jack@gmail.com");
viewModel.addNewContact(c1);
```
Creates a test contact(no ID passed - ROOM auto-generated it) and inserts it via the ViewModel.
The transcript notes the ID was removed from the constructor after it caused a unique constraint
error on repeated runs. 

```java
viewModel.getAllContacts().observe(this, new Observer<List<Contacts>>() {
```
Calls getAllContacts() which returns a LiveData object, then calls .observe() on it. This sets up a
listener anytime the database data changes, the code inside fires automatically. 

```java
public void onChanged(List<Contacts> contacts) {
    for(Contacts c: contacts){
        Log.v("TAGY", c.getName());
        contactsArrayList.add(c);
    }
```
onChanged fires whenever the LiveData updates. It loops through every contact returned from the database,
logs the name to Logcat (for testing), and adds each one to contactsArrayList.

```java
myAdapter.notifyDataSetChanged();
}
```
After updating the data list, this tells the RecyclerView that it's underlying data changed 
and it should re-render all visible items to reflect the new contacts.

```java
myAdapter = new MyAdapter(contactsArrayList);
recyclerView.setAdapter(myAdapter);
```
Re-creates the adapter with the now-populated list and attaches it to the RecyclerView, so the contacts
actually appear on screen.









