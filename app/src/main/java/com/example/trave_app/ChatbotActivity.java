package com.example.trave_app;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trave_app.chatbot.adapter.ChatAdapter;
import com.example.trave_app.chatbot.model.ChatMessage;
import com.example.trave_app.chatbot.service.GeminiAIService;
import com.example.trave_app.database.entity.Place;
import com.example.trave_app.database.entity.Favorite;
import com.example.trave_app.database.entity.SearchHistory;
import com.example.trave_app.repository.TravelRepository;
import androidx.lifecycle.ViewModelProvider;
import com.example.trave_app.viewmodel.TravelViewModel;

public class ChatbotActivity extends AppCompatActivity {
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageView backButton;
    private LinearLayout typingIndicator;
    private ChatAdapter chatAdapter;
    private GeminiAIService aiService;
    private TravelViewModel travelViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        initializeAIService();
        initializeServices();
        showWelcomeMessage();
    }

    private void initializeViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        backButton = findViewById(R.id.backButton);
        typingIndicator = findViewById(R.id.typingIndicator);
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void setupClickListeners() {
        sendButton.setOnClickListener(v -> sendMessage());
        backButton.setOnClickListener(v -> finish());
        
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void initializeAIService() {
        aiService = new GeminiAIService(this);
    }

    private void initializeServices() {
        travelViewModel = new ViewModelProvider(this).get(TravelViewModel.class);
    }

    private void showWelcomeMessage() {
        String welcomeMessage = "Hi, I'm Gemini. Ask me anything.";
        ChatMessage welcomeMsg = new ChatMessage(welcomeMessage, ChatMessage.TYPE_AI);
        chatAdapter.addMessage(welcomeMsg);
        scrollToBottom();
    }

    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (message.isEmpty()) return;

        // Add user message to chat
        ChatMessage userMessage = new ChatMessage(message, ChatMessage.TYPE_USER);
        chatAdapter.addMessage(userMessage);
        scrollToBottom();

        // Clear input and show typing indicator
        messageInput.setText("");
        showTypingIndicator(true);

        // Gemini-only: send the raw message directly (no local ML context)
        aiService.generateResponse(
                message,
                java.util.Collections.emptyList(),
                java.util.Collections.emptyList(),
                java.util.Collections.emptyList(),
                new GeminiAIService.AIResponseCallback() {
                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(() -> {
                            showTypingIndicator(false);
                            ChatMessage aiMessage = new ChatMessage(response, ChatMessage.TYPE_AI);
                            chatAdapter.addMessage(aiMessage);
                            scrollToBottom();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            showTypingIndicator(false);
                            ChatMessage msg = new ChatMessage("Sorry, I can't reply right now. Please try again in a moment.", ChatMessage.TYPE_AI);
                            chatAdapter.addMessage(msg);
                            scrollToBottom();
                        });
                    }
                }
        );
    }

    private void showTypingIndicator(boolean show) {
        typingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            scrollToBottom();
        }
    }

    private void scrollToBottom() {
        if (chatAdapter.getItemCount() > 0) {
            chatRecyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up observers
        if (travelViewModel != null) {
            travelViewModel.getAllPlaces().removeObservers(this);
            travelViewModel.getAllFavorites().removeObservers(this);
            travelViewModel.getAllSearchHistory().removeObservers(this);
        }
    }
}
