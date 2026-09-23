cmake_minimum_required(VERSION 3.22.1)

project("pocket_math_engine")

# 1. Native Intent Processor Library
add_library(
    native_intent_processor
    SHARED
    native_intent_processor.cpp
)

# 2. Pocket Math Core Advanced Engine Library
add_library(
    pocket_math_core
    SHARED
    pocket_math_core.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    native_intent_processor
    ${log-lib}
)

target_link_libraries(
    pocket_math_core
    ${log-lib}
)
