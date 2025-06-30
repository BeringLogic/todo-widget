package com.example.todowidget.network

import android.util.Log
import com.example.todowidget.model.Todo
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

interface TodoApiService {
    @GET("api/todos")
    suspend fun getTodos(): List<Todo>
    
    companion object {
        private const val TAG = "TodoApiService"
        private const val BASE_URL = "http://192.168.2.12:8081/"
        
        fun create(): TodoApiService {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            
            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()
                
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
                
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(TodoApiService::class.java)
        }
    }
}
