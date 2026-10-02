Drop plain-text workload files here to use with the standalone CLI's `file` command, e.g.:

```
cd backend
mvn -q -DskipTests package
java -cp target/classes com.adaptiveos.engine.Main file workloads/example.txt --gantt
```

File format (one process per line, `#` starts a comment):
```
P1 0 CPU:10
P2 1 CPU:3 IO:4 CPU:2
```
