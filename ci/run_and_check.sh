#!/bin/bash

# Script to launch http4s-pac4j-demo and verify the server starts correctly
# Usage: ./run_and_check.sh

set -e  # Stop script on error

echo "🚀 Starting http4s-pac4j-demo..."

# Go to project directory (one level up from ci/)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

# Determine which sbt command to use
if command -v sbt >/dev/null 2>&1; then
    SBT_CMD="sbt"
    echo "🛠️  Using system SBT command: $SBT_CMD"
else
    echo "📥 sbt command not found, using sbt wrapper..."
    # Download sbt launcher if not exists
    if [ ! -f "sbt" ]; then
        echo "📥 Downloading sbt launcher..."
        curl -L -o sbt "https://raw.githubusercontent.com/sbt/sbt/v1.9.6/sbt"
        chmod +x sbt
    fi
    SBT_CMD="./sbt"
    echo "🛠️  Using downloaded SBT wrapper: $SBT_CMD"
fi

# Clean any existing sbt processes to avoid conflicts
echo "🧹 Cleaning up any existing sbt processes..."
pkill -f "sbt" >/dev/null 2>&1 || true

# Clean and compile project
echo "📦 Compiling project..."
$SBT_CMD clean compile

# Ensure target directory exists
mkdir -p target

# Start server in background
echo "🌐 Starting server..."
$SBT_CMD "runMain com.test.MainIO" > target/server.log 2>&1 &
SERVER_PID=$!

# Function to cleanup server on exit
cleanup() {
    echo "🧹 Cleaning up..."
    kill $SERVER_PID 2>/dev/null || true
    wait $SERVER_PID 2>/dev/null || true
    echo "🛑 Server stopped"
}
trap cleanup EXIT

# Wait for server to start (maximum 60 seconds)
echo "⏳ Waiting for server startup..."
for i in {1..60}; do
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080 | grep -q "200"; then
        echo "✅ Server started successfully!"
        break
    fi
    if [ $i -eq 60 ]; then
        echo "❌ Timeout: Server did not start within 60 seconds"
        echo "📋 Server logs:"
        cat target/server.log
        exit 1
    fi
    sleep 1
done

# Verify application responds correctly
echo "🔍 Verifying HTTP response..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080)

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ Application responds with HTTP 200"
    echo "🌐 Application accessible at: http://localhost:8080"
    
    # Get and display basic page content to verify it's working
    echo "📄 Fetching homepage content..."
    HOMEPAGE_CONTENT=$(curl -s http://localhost:8080 2>/dev/null || echo "Failed to fetch content")
    
    if echo "$HOMEPAGE_CONTENT" | grep -q "http4s-pac4j-demo"; then
        echo "✅ Homepage contains expected demo title"
    else
        echo "⚠️  Homepage content may be different than expected"
    fi
    
    echo "🎉 http4s-pac4j-demo test completed successfully!"
    echo "✅ Server is running and responding correctly"
    echo "📝 To test authentication manually, visit: http://localhost:8080"
else
    echo "❌ Server test failed! HTTP code received: $HTTP_CODE"
    echo "📋 Server logs:"
    cat target/server.log
    exit 1
fi
