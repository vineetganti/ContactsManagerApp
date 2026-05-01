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






