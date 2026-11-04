#!/bin/bash

#!/bin/bash

echo "=== 1. Health Check ==="
curl -s -o /dev/null -w "user-service: %{http_code}\n" http://localhost:8081/api/members/health
curl -s -o /dev/null -w "course-service: %{http_code}\n" http://localhost:8082/actuator/health
curl -s -o /dev/null -w "booking-service: %{http_code}\n" http://localhost:8083/actuator/health

echo -e "\n=== 2. Add Test Data ==="
curl -X POST "http://localhost:8081/api/members" -H "Content-Type: application/json" -d '{"name":"程子恒","remainingSessions":5}'
curl -X POST "http://localhost:8082/api/courses" -H "Content-Type: application/json" -d '{"name":"动感单车","capacity":10,"bookedCount":0}'

echo -e "\n=== 3. Normal Booking ==="
curl -X POST "http://localhost:8083/api/book?memberId=1&courseId=1"

echo -e "\n=== 4. Failure Scenario ==="
echo "Stopping user-service..."
docker stop user-service
sleep 2
echo "Attempting booking (should fail)..."
curl -X POST "http://localhost:8083/api/book?memberId=1&courseId=1"
echo ""

echo "Starting user-service..."
docker start user-service