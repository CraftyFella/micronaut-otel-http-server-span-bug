## @NewSpan annotation IS set as parent for new spans

This project demos how with the micronaut 3 and OTEL the @NewSpan annotation works as expected.

## To Run the test

To see the issue please run

```bash
./gradlew test --info
```

## To Run the app

In the app you'll make a request and see the trace created as expected.

### Dependencies

You'll also need to start something like jaeger using

```bash
docker run --name jaeger3 -e COLLECTOR_OTLP_ENABLED=true -p 16686:16686 -p 4317:4317 -p 4318:4318 jaegertracing/all-in-one:1.35
```
Now start the app
```bash
./gradlew run
```

Make a request to see books by id

```bash
curl --location 'http://localhost:8085/api/books/book-1'
```

then browse to [http://localhost:16686/](http://localhost:16686/) find the app in the drop down and click Find Traces button. You'll then see that instead of single trace you have 2