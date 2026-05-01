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






