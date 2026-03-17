import React, { useState, useRef, useEffect } from "react";
import { ChatApi, ChatMessage } from "../api";
import { HiChat, HiX, HiPaperAirplane } from "react-icons/hi";

export default function ChatBot() {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleSend = async () => {
    if (!input.trim() || loading) return;

    const userMessage: ChatMessage = {
      role: "user",
      content: input.trim(),
    };

    // Add user message immediately for better UX
    const updatedMessages = [...messages, userMessage];
    setMessages(updatedMessages);
    setInput("");
    setLoading(true);

    try {
      console.log("Sending chat message:", userMessage.content);
      console.log("Conversation history length:", messages.length);
      
      const response = await ChatApi.sendMessage({
        message: userMessage.content,
        conversationHistory: messages, // Send history WITHOUT the current user message
      });

      console.log("Chat response received:", response);

      // Update messages with the full conversation history from response
      if (response && response.conversationHistory && response.conversationHistory.length > 0) {
        setMessages(response.conversationHistory);
      } else if (response && response.response) {
        // Fallback: if conversationHistory is missing, use the response text
        const assistantMessage: ChatMessage = {
          role: "assistant",
          content: response.response,
        };
        setMessages([...updatedMessages, assistantMessage]);
      } else {
        throw new Error("Invalid response format");
      }
    } catch (error: any) {
      console.error("Chat error:", error);
      console.error("Error response:", error.response);
      console.error("Error status:", error.response?.status);
      console.error("Error data:", error.response?.data);
      
      let errorContent = "Sorry, I encountered an error. Please try again later.";
      
      if (error.response?.status === 401) {
        errorContent = "Please log in to use the chat feature.";
      } else if (error.response?.status === 403) {
        errorContent = "You don't have permission to use the chat.";
      } else if (error.response?.data?.message) {
        errorContent = error.response.data.message;
      } else if (error.response?.data?.error) {
        errorContent = error.response.data.error;
      } else if (error.message) {
        errorContent = error.message;
      }
      
      const errorMessage: ChatMessage = {
        role: "assistant",
        content: errorContent,
      };
      setMessages([...updatedMessages, errorMessage]);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <>
      {/* Chat Button */}
      {!isOpen && (
        <button
          onClick={() => setIsOpen(true)}
          className="fixed bottom-6 right-6 w-14 h-14 bg-blue-600 hover:bg-blue-700 text-white rounded-full shadow-lg flex items-center justify-center transition-all duration-300 z-50"
          aria-label="Open chat"
        >
          <HiChat className="w-6 h-6" />
        </button>
      )}

      {/* Chat Window */}
      {isOpen && (
        <div className="fixed bottom-6 right-6 w-96 h-[600px] bg-gray-800 rounded-lg shadow-2xl border border-gray-700 flex flex-col z-50">
          {/* Header */}
          <div className="bg-gradient-to-r from-blue-600 to-cyan-500 p-4 rounded-t-lg flex items-center justify-between">
            <div>
              <h3 className="text-white font-semibold text-lg">SuperDoc Assistant</h3>
            </div>
            <button
              onClick={() => setIsOpen(false)}
              className="text-white hover:text-gray-200 transition-colors p-1"
              aria-label="Close chat"
            >
              <HiX className="w-5 h-5" />
            </button>
          </div>

          {/* Messages */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4">
            {messages.length === 0 && (
              <div className="text-center text-gray-400 mt-8">
                <HiChat className="w-12 h-12 mx-auto mb-3 opacity-50" />
                <p className="text-sm">Hello! How can I help you today?</p>
                <p className="text-xs mt-2 text-gray-500">
                  I can help with booking appointments, managing your profile, and more.
                </p>
              </div>
            )}

            {messages.map((message, index) => (
              <div
                key={index}
                className={`flex ${message.role === "user" ? "justify-end" : "justify-start"}`}
              >
                <div
                  className={`max-w-[80%] rounded-lg px-4 py-2 ${
                    message.role === "user"
                      ? "bg-blue-600 text-white"
                      : "bg-gray-700 text-gray-100"
                  }`}
                >
                  <p className="text-sm whitespace-pre-wrap">{message.content}</p>
                </div>
              </div>
            ))}

            {loading && (
              <div className="flex justify-start">
                <div className="bg-gray-700 text-gray-100 rounded-lg px-4 py-2">
                  <div className="flex space-x-1">
                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce"></div>
                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: "0.2s" }}></div>
                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: "0.4s" }}></div>
                  </div>
                </div>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          {/* Input */}
          <div className="border-t border-gray-700 p-4">
            <div className="flex space-x-2">
              <input
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyPress={handleKeyPress}
                placeholder="Type your message..."
                disabled={loading}
                className="flex-1 px-4 py-2 bg-gray-700 text-white rounded-lg border border-gray-600 focus:border-blue-500 focus:outline-none disabled:opacity-50"
              />
              <button
                onClick={handleSend}
                disabled={loading || !input.trim()}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center"
              >
                <HiPaperAirplane className="w-5 h-5" />
              </button>
            </div>
            <p className="text-xs text-gray-500 mt-2 text-center">
              Note: This assistant cannot provide medical advice. Please consult a doctor for medical concerns.
            </p>
          </div>
        </div>
      )}
    </>
  );
}

