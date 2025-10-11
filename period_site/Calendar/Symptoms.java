public class Symptoms {

    public String getSymptoms(String cyclePhase) {  
        if (cyclePhase.equals("Menstrual Phase")) {
            return "Common symptoms during the menstrual phase include cramps, bloating, mood swings, and fatigue. It's important to stay hydrated and get plenty of rest.";
        } else if (cyclePhase.equals("Follicular Phase")) {
            return "During the follicular phase, you may experience increased energy levels and improved mood. This is a great time to focus on personal goals and self-care.";
        } else if (cyclePhase.equals("Ovulation Phase")) {
            return "In the ovulation phase, some people notice heightened senses and increased libido. It's also a time when you may feel more social and outgoing.";
        } else if (cyclePhase.equals("Luteal Phase")) {
            return "The luteal phase can bring about symptoms such as breast tenderness, irritability, and food cravings. Maintaining a balanced diet and managing stress can help alleviate these symptoms.";
        } else {
            return "As you approach a new cycle, you might experience a mix of symptoms from the previous phases. Keeping track of your symptoms can help you better understand your cycle.";
        }
    }
}