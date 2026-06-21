package com.zehranur.deutschifyapp.network;

import com.zehranur.deutschifyapp.model.AiFeedbackRequest;
import com.zehranur.deutschifyapp.model.AiFeedbackResponse;
import com.zehranur.deutschifyapp.model.AnswerRequest;
import com.zehranur.deutschifyapp.model.GrammarCheckRequest;
import com.zehranur.deutschifyapp.model.GrammarCheckResponse;
import com.zehranur.deutschifyapp.model.GrammarTopic;
import com.zehranur.deutschifyapp.model.GrammarTopicDetail;
import com.zehranur.deutschifyapp.model.HistoryResponse;
import com.zehranur.deutschifyapp.model.MeResponse;
import com.zehranur.deutschifyapp.model.StatsResponse;
import com.zehranur.deutschifyapp.model.AnswerResponse;
import com.zehranur.deutschifyapp.model.LoginRequest;
import com.zehranur.deutschifyapp.model.LoginResponse;
import com.zehranur.deutschifyapp.model.QueueResponse;
import com.zehranur.deutschifyapp.model.RegisterRequest;
import com.zehranur.deutschifyapp.model.RegisterResponse;
import com.zehranur.deutschifyapp.model.ReviewResponse;
import com.zehranur.deutschifyapp.model.WordResponse;
import java.util.List;
import com.zehranur.deutschifyapp.model.SessionStartResponse;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @GET("study/queue/{user_id}")
    Call<QueueResponse> getStudyQueue(@Header("Authorization") String token, @Path("user_id") int userId, @Query("limit") int limit);

    @POST("study/answer")
    Call<AnswerResponse> submitAnswer(@Header("Authorization") String token, @Body AnswerRequest body);

    @POST("study/session/start")
    Call<SessionStartResponse> startSession(@Header("Authorization") String token, @Body java.util.Map<String, Integer> body);

    @PUT("study/session/{session_id}/end")
    Call<Void> endSession(@Header("Authorization") String token, @Path("session_id") int sessionId);

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest body);

    @POST("auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest body);

    @GET("tts/{word}")
    Call<ResponseBody> getWordAudio(@Path("word") String word,
                                    @Header("Authorization") String token);

    @GET("auth/me")
    Call<MeResponse> getMe(@Header("Authorization") String token);

    @GET("study/stats")
    Call<StatsResponse> getStats(@Header("Authorization") String token);

    @GET("study/history")
    Call<HistoryResponse> getHistory(@Header("Authorization") String token);

    @GET("words/daily")
    Call<WordResponse> getDailyWord();

    @GET("words/")
    Call<List<WordResponse>> getWords(@Query("has_article") boolean hasArticle);

    @GET("study/review")
    Call<ReviewResponse> getReviewWords(@Header("Authorization") String token);

    @GET("grammar/topics")
    Call<List<GrammarTopic>> getGrammarTopics();

    @GET("grammar/topics/{slug}/exercises")
    Call<GrammarTopicDetail> getGrammarTopicDetail(@Path("slug") String slug);

    @POST("grammar/check")
    Call<GrammarCheckResponse> checkGrammarAnswer(@Body GrammarCheckRequest body);

    @POST("study/feedback")
    Call<AiFeedbackResponse> getAiFeedback(@Header("Authorization") String token,
                                            @Body AiFeedbackRequest body);

    @POST("study/example")
    Call<com.zehranur.deutschifyapp.model.ExampleResponse> getExampleSentence(
            @Header("Authorization") String token,
            @Body com.zehranur.deutschifyapp.model.ExampleRequest request);
}