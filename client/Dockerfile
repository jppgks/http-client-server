FROM jppgks/http:latest

ENV METHOD "GET"
ENV HOST "localhost"
ENV PORT "8080"

WORKDIR client/
CMD ["sh", "-c", "gradle run -Pmethod=${METHOD} -Phost=${HOST} -Pport=${PORT} -q"]

