package com.example.trave_app;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trave_app.chatbot.adapter.ChatAdapter;
import com.example.trave_app.chatbot.model.ChatMessage;
import com.example.trave_app.chatbot.service.LocalTravelAssistantService;

public class LocalAssistantActivity extends AppCompatActivity {
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageView backButton;
    private LinearLayout typingIndicator;
    private ChatAdapter chatAdapter;
    private LocalTravelAssistantService localService;

    private enum OnboardingStep {
        WELCOME,
        ASK_TRIP_TYPE,
        ASK_DESTINATION,
        ASK_DOM_INTL,
        ASK_BUDGET,
        ASK_PREFERENCE,
        ASK_PRICE_FILTER,
        COMPLETE
    }

    private static class TravelPlan {
        String tripType; // vacation / business / family
        String destinationPref; // user specified or request suggestions
        String domesticOrInternational;
        String budgetApprox; // free text or number
        String preferenceLevel; // budget-friendly / luxury
        String priceFilter; // e.g., per-night filter or yes/no
    }

    private OnboardingStep currentStep = OnboardingStep.WELCOME;
    private final TravelPlan plan = new TravelPlan();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_assistant);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        backButton = findViewById(R.id.backButton);
        typingIndicator = findViewById(R.id.typingIndicator);

        chatAdapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        localService = new LocalTravelAssistantService(this);

        sendButton.setOnClickListener(v -> sendMessage());
        backButton.setOnClickListener(v -> finish());
        messageInput.setOnEditorActionListener((v, actionId, event) -> { sendMessage(); return true; });

        // Start guided flow: greeting + first question
        chatAdapter.addMessage(new ChatMessage("Hello Sir! Great to see here you.", ChatMessage.TYPE_AI));
        chatAdapter.addMessage(new ChatMessage("Are you planning a vacation, business trip, or a family visit?", ChatMessage.TYPE_AI));
        currentStep = OnboardingStep.ASK_TRIP_TYPE;
        scrollToBottom();
    }

    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(message)) return;

        chatAdapter.addMessage(new ChatMessage(message, ChatMessage.TYPE_USER));
        scrollToBottom();
        messageInput.setText("");
        showTypingIndicator(true);
        handleConversation(message);
    }

    private void showTypingIndicator(boolean show) {
        typingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) scrollToBottom();
    }

    private void scrollToBottom() {
        if (chatAdapter.getItemCount() > 0) {
            chatRecyclerView.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        }
    }

    private void handleConversation(String userText) {
        String answer;
        switch (currentStep) {
            case ASK_TRIP_TYPE:
                plan.tripType = userText;
                showTypingIndicator(false);
                chatAdapter.addMessage(new ChatMessage("Do you have any preferred destination or should I suggest some options?", ChatMessage.TYPE_AI));
                currentStep = OnboardingStep.ASK_DESTINATION;
                scrollToBottom();
                return;
            case ASK_DESTINATION:
                plan.destinationPref = userText;
                showTypingIndicator(false);
                chatAdapter.addMessage(new ChatMessage("Do you want to travel domestically or internationally?", ChatMessage.TYPE_AI));
                currentStep = OnboardingStep.ASK_DOM_INTL;
                scrollToBottom();
                return;
            case ASK_DOM_INTL:
                plan.domesticOrInternational = userText;
                showTypingIndicator(false);
                chatAdapter.addMessage(new ChatMessage("What is your approximate travel budget?", ChatMessage.TYPE_AI));
                currentStep = OnboardingStep.ASK_BUDGET;
                scrollToBottom();
                return;
            case ASK_BUDGET:
                plan.budgetApprox = userText;
                showTypingIndicator(false);
                chatAdapter.addMessage(new ChatMessage("Do you prefer budget-friendly options or luxury travel?", ChatMessage.TYPE_AI));
                currentStep = OnboardingStep.ASK_PREFERENCE;
                scrollToBottom();
                return;
            case ASK_PREFERENCE:
                plan.preferenceLevel = userText;
                showTypingIndicator(false);
                chatAdapter.addMessage(new ChatMessage("Should I filter stays under a certain price range? (e.g., per night cost)", ChatMessage.TYPE_AI));
                currentStep = OnboardingStep.ASK_PRICE_FILTER;
                scrollToBottom();
                return;
            case ASK_PRICE_FILTER:
                plan.priceFilter = userText;
                showTypingIndicator(false);
                chatAdapter.addMessage(new ChatMessage("Thanks! You can now ask for suggestions (e.g., 'prefer me hotels' or 'list restaurants in Navi Mumbai').", ChatMessage.TYPE_AI));
                currentStep = OnboardingStep.COMPLETE;
                scrollToBottom();
                return;
            case COMPLETE:
            default:
                // Normal Q&A mode using local service
                answer = localService.answer(userText);
                showTypingIndicator(false);
                chatAdapter.addMessage(new ChatMessage(answer, ChatMessage.TYPE_AI));
                scrollToBottom();
        }
    }
}
