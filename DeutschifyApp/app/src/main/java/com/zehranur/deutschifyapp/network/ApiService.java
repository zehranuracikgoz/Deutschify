package com.zehranur.deutschifyapp.network;

import com.zehranur.deutschifyapp.model.AnswerRequest;
import com.zehranur.deutschifyapp.model.AnswerResponse;
import com.zehranur.deutschifyapp.model.LoginRequest;
import com.zehranur.deutschifyapp.model.LoginResponse;
import com.zehranur.deutschifyapp.model.QueueResponse;
import com.zehranur.deutschifyapp.model.RegisterRequest;
import com.zehranur.deutschifyapp.model.RegisterResponse;
import com.zehranur.deutschifyapp.model.SessionStartResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @GET("study/queue/{user_id}")
    Call<QueueResponse> getStudyQueue(@Path("user_id") int userId, @Query("limit") int limit);

    @POST("study/answer")
    Call<AnswerResponse> submitAnswer(@Body AnswerRequest body);

    @POST("study/session/start")
    Call<SessionStartResponse> startSession(@Body java.util.Map<String, Integer> body);

    @PUT("study/session/{session_id}/end")
    Call<Void> endSession(@Path("session_id") int sessionId);

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest body);

    @POST("auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest body);
}