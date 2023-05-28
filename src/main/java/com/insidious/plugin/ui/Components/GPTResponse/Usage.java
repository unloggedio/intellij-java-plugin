package com.insidious.plugin.ui.Components.GPTResponse;

public class Usage{
    public int prompt_tokens;
    public int completion_tokens;
    public int total_tokens;

    public int getPrompt_tokens() {
        return prompt_tokens;
    }

    public void setPrompt_tokens(int prompt_tokens) {
        this.prompt_tokens = prompt_tokens;
    }

    public int getCompletion_tokens() {
        return completion_tokens;
    }

    public void setCompletion_tokens(int completion_tokens) {
        this.completion_tokens = completion_tokens;
    }

    public int getTotal_tokens() {
        return total_tokens;
    }

    public void setTotal_tokens(int total_tokens) {
        this.total_tokens = total_tokens;
    }

    public Usage() {
    }

    public Usage(int prompt_tokens, int completion_tokens, int total_tokens) {
        this.prompt_tokens = prompt_tokens;
        this.completion_tokens = completion_tokens;
        this.total_tokens = total_tokens;
    }

    @Override
    public String toString() {
        return "Usage{" +
                "prompt_tokens=" + prompt_tokens +
                ", completion_tokens=" + completion_tokens +
                ", total_tokens=" + total_tokens +
                '}';
    }
}
