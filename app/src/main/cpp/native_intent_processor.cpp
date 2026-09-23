#include <jni.h>
#include <string>
#include <regex>
#include <vector>
#include <algorithm>
#include <android/log.h>

#define LOG_TAG "NativeIntentProcessor"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

/**
 * Native C++ Intent Formatter
 * Pre-processes input prompts and system prompts to enforce direct-response intent,
 * eliminating model tendency to produce meta-breakdowns ("Here is a suggested response:").
 */
JNIEXPORT jstring JNICALL
Java_com_example_engine_NativeIntentProcessor_nativeFormatPrompt(
        JNIEnv *env,
        jobject /* this */,
        jstring user_prompt_jstr,
        jstring system_prompt_jstr,
        jboolean is_arabic
) {
    const char *user_prompt_chars = env->GetStringUTFChars(user_prompt_jstr, nullptr);
    const char *system_prompt_chars = env->GetStringUTFChars(system_prompt_jstr, nullptr);

    std::string user_prompt = user_prompt_chars ? user_prompt_chars : "";
    std::string system_prompt = system_prompt_chars ? system_prompt_chars : "";

    env->ReleaseStringUTFChars(user_prompt_jstr, user_prompt_chars);
    env->ReleaseStringUTFChars(system_prompt_jstr, system_prompt_chars);

    std::string enhanced_system_prompt = system_prompt;

    // Inject strict C++ intent guardrails into system prompt
    if (is_arabic) {
        enhanced_system_prompt += "\n\n[تعليمات جدار الحماية للنية - C++ Native Guardrails]:\n"
                                  "1. أجب بأسلوب مباشر، دقيق، ومهني باللغة العربية.\n"
                                  "2. يُمنع تماماً إضافة مخرجات تحليلية جانبية أو عناوين مثل '**Here is a suggested response:**' أو '**Here is a breakdown:**'.\n"
                                  "3. قدّم الإجابة المباشرة فقط للمستخدم دون شرح سبب اختيارك لهذه الإجابة أو تحليلها بلغات أخرى.";
    } else {
        enhanced_system_prompt += "\n\n[C++ Native Intent Guardrails]:\n"
                                  "1. Respond directly, concisely, and professionally.\n"
                                  "2. NEVER output meta-commentary, suggested response headers (e.g. '**Here is a suggested response:**'), or breakdown analyses.\n"
                                  "3. Output ONLY the direct answer to the user's intent.";
    }

    return env->NewStringUTF(enhanced_system_prompt.c_str());
}

/**
 * Native C++ Output Sanitizer
 * Scans generated output text from 5B parameter models and strips meta commentary,
 * headers like "Here is a breakdown of...", "This response is excellent because...",
 * and extracts the core direct answer.
 */
JNIEXPORT jstring JNICALL
Java_com_example_engine_NativeIntentProcessor_nativeSanitizeOutput(
        JNIEnv *env,
        jobject /* this */,
        jstring raw_output_jstr
) {
    const char *raw_chars = env->GetStringUTFChars(raw_output_jstr, nullptr);
    std::string text = raw_chars ? raw_chars : "";
    env->ReleaseStringUTFChars(raw_output_jstr, raw_chars);

    if (text.empty()) {
        return env->NewStringUTF("");
    }

    // Preserve <think>...</think> block if present
    std::string think_block = "";
    size_t think_start = text.find("<think>");
    size_t think_end = text.find("</think>");
    
    if (think_start != std::string::npos && think_end != std::string::npos && think_end > think_start) {
        think_block = text.substr(think_start, (think_end + 8) - think_start);
        text = text.substr(think_end + 8);
    }

    // Patterns commonly generated when small (e.g. 5B) models leak tutor/evaluator meta-data
    std::vector<std::string> meta_headers = {
        "**Here is a suggested response:**",
        "**Here is a suggested response**",
        "Here is a suggested response:",
        "**Here is a breakdown of the suggested response:**",
        "**Here is a breakdown of the suggested response**",
        "Here is a breakdown of the suggested response:",
        "This response is excellent because",
        "A warm and polite opening",
        "So that I can help you better",
        "This is the crucial part"
    };

    // Remove exact match headers if they appear as line prefixes
    for (const auto &header : meta_headers) {
        size_t pos = 0;
        while ((pos = text.find(header, pos)) != std::string::npos) {
            // If there's meta breakdown following, check if we can isolate the actual response
            text.erase(pos, header.length());
        }
    }

    // Regex cleaning for breakdown patterns like "**Here is a breakdown...**" until end of text
    std::regex breakdown_regex(R"(\*\*Here is a breakdown of the suggested response:\*\*[\s\S]*)");
    text = std::regex_replace(text, breakdown_regex, "");

    // Regex cleaning for "This response is excellent because..."
    std::regex excellent_regex(R"(This response is excellent because[\s\S]*)");
    text = std::regex_replace(text, excellent_regex, "");

    // Trim leading and trailing whitespace
    size_t start = text.find_first_not_of(" \t\n\r");
    size_t end = text.find_last_not_of(" \t\n\r");

    std::string cleaned_body = (start == std::string::npos) ? "" : text.substr(start, end - start + 1);

    std::string result = think_block.empty() ? cleaned_body : (think_block + "\n\n" + cleaned_body);

    return env->NewStringUTF(result.c_str());
}

} // extern "C"
