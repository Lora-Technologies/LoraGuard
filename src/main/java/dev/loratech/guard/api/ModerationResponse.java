package dev.loratech.guard.api;

import java.util.List;
import java.util.Map;

public class ModerationResponse {

    private String id;
    private String model;
    private List<Result> results;
    private String warning;

    public String getId() {
        return id;
    }

    public String getModel() {
        return model;
    }

    public List<Result> getResults() {
        return results;
    }

    public String getWarning() {
        return warning;
    }

    public boolean hasWarning() {
        return warning != null && !warning.isEmpty();
    }

    public static class Result {
        private boolean flagged;
        private Map<String, Boolean> categories;
        private Map<String, Double> category_scores;
        private String error;

        public boolean isFlagged() {
            return flagged;
        }

        public Map<String, Boolean> getCategories() {
            return categories;
        }

        public Map<String, Double> getCategoryScores() {
            return category_scores;
        }

        public String getError() {
            return error;
        }

        public boolean hasError() {
            return error != null && !error.isEmpty();
        }

        public String getHighestCategory() {
            if (category_scores == null || category_scores.isEmpty()) {
                return "unknown";
            }
            
            String highest = null;
            double highestScore = 0;
            
            for (Map.Entry<String, Double> entry : category_scores.entrySet()) {
                if (entry.getValue() > highestScore) {
                    highestScore = entry.getValue();
                    highest = entry.getKey();
                }
            }
            
            return highest != null ? highest : "unknown";
        }

        public double getHighestScore() {
            if (category_scores == null || category_scores.isEmpty()) {
                return 0;
            }
            
            return category_scores.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0);
        }

        public List<String> getFlaggedCategories() {
            if (categories == null) {
                return List.of();
            }
            
            return categories.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .toList();
        }
    }
}
