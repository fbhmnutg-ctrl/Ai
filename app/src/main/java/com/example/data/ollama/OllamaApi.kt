package com.example.data.ollama

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Streaming

interface OllamaApi {
    @GET("api/version")
    suspend fun getVersion(): Response<OllamaVersionResponse>

    @GET("api/tags")
    suspend fun getTags(): Response<OllamaTagsResponse>

    @Streaming
    @POST("api/chat")
    suspend fun streamChat(@Body request: OllamaChatRequest): Response<ResponseBody>

    @Streaming
    @POST("api/pull")
    suspend fun streamPull(@Body request: OllamaPullRequest): Response<ResponseBody>
}
