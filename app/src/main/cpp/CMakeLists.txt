cmake_minimum_required(VERSION 3.22.1)
project("tivot_native")

set(CMAKE_CXX_STANDARD 17)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -ffast-math")
endif()

# Check for llama.cpp existence
if(EXISTS "${CMAKE_SOURCE_DIR}/llama.cpp/CMakeLists.txt")
    set(LLAMA_BUILD_TESTS OFF CACHE BOOL "Disable llama tests" FORCE)
    set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "Disable llama examples" FORCE)
    set(LLAMA_BUILD_SERVER OFF CACHE BOOL "Disable llama server" FORCE)
    add_subdirectory(llama.cpp)
    set(LLAMA_AVAILABLE TRUE)
    add_definitions(-DLLAMA_AVAILABLE=1)
else()
    set(LLAMA_AVAILABLE FALSE)
endif()

add_library(
        tivot_native
        SHARED
        native-lib.cpp)

find_library(
        log-lib
        log)

if(LLAMA_AVAILABLE)
    target_include_directories(tivot_native PRIVATE llama.cpp/include llama.cpp/ggml/include)
    target_link_libraries(tivot_native llama ggml ${log-lib} android)
else()
    target_link_libraries(tivot_native ${log-lib} android)
endif()
