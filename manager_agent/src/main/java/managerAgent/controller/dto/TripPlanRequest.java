package managerAgent.controller.dto;

/**
 * Trip plan request payload
 */
public class TripPlanRequest {

    private String origin;
    private String destination;
    private String travelDate;
    private String preferences;
    private String budget;
    private String transportMode;
    private String extraRequirements;

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getTravelDate() {
        return travelDate;
    }

    public void setTravelDate(String travelDate) {
        this.travelDate = travelDate;
    }

    public String getPreferences() {
        return preferences;
    }

    public void setPreferences(String preferences) {
        this.preferences = preferences;
    }

    public String getBudget() {
        return budget;
    }

    public void setBudget(String budget) {
        this.budget = budget;
    }

    public String getTransportMode() {
        return transportMode;
    }

    public void setTransportMode(String transportMode) {
        this.transportMode = transportMode;
    }

    public String getExtraRequirements() {
        return extraRequirements;
    }

    public void setExtraRequirements(String extraRequirements) {
        this.extraRequirements = extraRequirements;
    }
}
