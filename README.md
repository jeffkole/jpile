**What is jPile?**

A project developed at opower that uses `javax.persistence` annotations to load objects in MySQL using its infile stream format. This component is meant for importing large amount of data at a high throughput rate. It supports many of the same features Hibernate supports except with up to _10x_ performance gain. So for example, if your data takes 60 seconds to be imported to MySQL database, with jPile it would only take 6 seconds! You don't have to change anything on your model objects. jPile will read the persistence annotations automatically at start up. 

The status of the project is still beta.

**What are annotations are supported?**

The following annotations are supported:

* @Table
* @SecondaryTable
* @SecondaryTables
* @Embedded
* @Id
* @Column
* @OneToMany
* @ManyToOne
* @OneToOne
* @JoinColumn
* @PrimaryKeyJoinColumn
* @GeneratedValue

**How does jPile handle ids?**

jPile cannot rely on MySQL `auto_generated` option. Typical database operation saves a new row and fetches the last auto generated id.  This is not possible when flushing an infile stream to the database. Instead jPile tries to generate its own auto generated ids for any column defintion that has `@GeneratedValue(strategy = GenerationType.AUTO)`. 

**How do I run the tests?**

jPile needs a local MySQL running and Apache Maven. Create a new schema called 'jpile' and import the sql file located at `src/test/db/jpile.sql`. The test classes use `root` with no password to login. After creating the local database, you should be able to run `mvn clean install` to run all the tests and install locally. 

**What do I do if I find a bug?**

The project is still under development. One of the reasons we decided to go open source was so that other people could improve this project. If you find any bugs, please create a new issue or contact the lead developer on the project. 

**How do I use jPile?**

jPile is very to use. If you are using Maven, then add the following dependency:

```
<dependency>
    <groupId>com.opower.jpile</groupId>
    <artifactId>jpile</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

The most common use case is to create a new instance of `HierarchicalInfileObjectLoader`. You have to provide a valid database `Connection`. `HierarchicalInfileObjectLoader` doesn't rely on a database pool because it needs to disable foreign keys constraints. Using multiple connections would fail because each new connection would have foreign keys enabled by default. Below shows how to do this.

```
Connection connection = ...;
HierarchicalInfileObjectLoader hierarchicalInfileObjectLoader = new HierarchicalInfileObjectLoader();

try {
  hierarchicalInfileObjectLoader.setConnection(connection);
  
  hierarchicalInfileObjectLoader.persit(myEntity);
  // Add more using persist()
} finally {
  hierarchicalInfileObjectLoader.close();
  connection.close(); // Don't forget to close the connection
}
```

**What license is jPile released under?**

jPile is released on the MIT license which is available in `license.txt` to read. 


**How was the performance comparison done?**

25,000 fake objects were created. Each object has a Customer, Contact (One-to-one) and 4 Producs (One-to-many). All these objects were saved using simple MySQL preppared statements, Hibernate, and jPile. The results were as follows:

* Prepared Statments,  60s
* Hibernate         ,  40s                     
* jPile             ,  6s

There is a chart is at http://cloud.github.com/downloads/opower/jpile/performance-graph.png. 

![Performance Graph]('http://cloud.github.com/downloads/opower/jpile/performance-graph.png')

