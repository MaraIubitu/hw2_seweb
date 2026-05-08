// Chatbot functionality
let currentPageContext = '';

document.addEventListener('DOMContentLoaded', function() {
    // Read any server-provided page context before initializing
    const ctxEl = document.getElementById('page-context');
    if (ctxEl && ctxEl.value) {
        currentPageContext = ctxEl.value;
    }
    initializeChatbot();
});

function initializeChatbot() {
    // Initialize chat by calling the backend
    fetch('/api/chat/init', { method: 'POST' })
        .then(response => response.json())
        .catch(error => console.error('Error initializing chat:', error));

    // Load initial conversation starters
    loadConversationStarters();
}

function toggleChatbot() {
    const widget = document.getElementById('chatbot-widget');
    if (widget) {
        widget.classList.toggle('active');
    }
}

function loadConversationStarters() {
    fetch(`/api/chat/starters?pageContext=${encodeURIComponent(currentPageContext)}`)
        .then(response => response.json())
        .then(starters => {
            const startersContainer = document.getElementById('chat-starters');
            if (startersContainer) {
                startersContainer.innerHTML = '';
                starters.forEach(starter => {
                    const button = document.createElement('button');
                    button.className = 'chat-starter-btn';
                    button.textContent = starter;
                    button.onclick = () => {
                        document.getElementById('chat-input').value = starter;
                        sendChatMessage();
                    };
                    startersContainer.appendChild(button);
                });
            }
        })
        .catch(error => console.error('Error loading starters:', error));
}

function sendChatMessage() {
    const input = document.getElementById('chat-input');
    const message = input.value.trim();

    if (!message) return;

    // Add user message to chat
    addChatMessage(message, 'user');
    input.value = '';

    // Send message to backend
    fetch('/api/chat/message', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            message: message,
            pageContext: currentPageContext
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.message) {
            addChatMessage(data.message, 'bot');
        } else if (data.error) {
            addChatMessage('Error: ' + data.error, 'bot');
        }
    })
    .catch(error => {
        console.error('Error sending message:', error);
        addChatMessage('Sorry, I encountered an error. Please try again.', 'bot');
    });
}

function addChatMessage(text, sender) {
    const messagesContainer = document.getElementById('chat-messages');
    if (!messagesContainer) return;

    const messageDiv = document.createElement('div');
    messageDiv.className = `chat-message ${sender}`;
    messageDiv.textContent = text;
    messagesContainer.appendChild(messageDiv);

    // Scroll to bottom
    const chatBody = document.getElementById('chatbot-body');
    if (chatBody) {
        chatBody.scrollTop = chatBody.scrollHeight;
    }
}

function handleChatKeypress(event) {
    if (event.key === 'Enter') {
        sendChatMessage();
    }
}

// Update page context when navigating
function setPageContext(context) {
    currentPageContext = context;
    loadConversationStarters();
}
