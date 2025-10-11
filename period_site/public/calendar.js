let periodStartDate = prompt("Enter The Start of Your Last Period: ");
console.log ("Your Period Started " + periodStartDate);

let lastPeriod = localStorage.getItem('lastPeriod')
  ? new Date(localStorage.getItem('lastPeriod'))
  : null;

document.getElementById('mark-start').addEventListener('click', () => {
  const today = new Date();
  localStorage.setItem('lastPeriod', today.toISOString());
  lastPeriod = today;
  updateUI();
});

function calculateNextPeriod() {
  if (!lastPeriod) return null;
  return new Date(lastPeriod.getTime() + 28 * 24 * 60 * 60 * 1000);
}

function getCycleDay() {
  if (!lastPeriod) return null;
  const diff = new Date() - lastPeriod;
  return Math.floor(diff / (1000 * 60 * 60 * 24));
}

function getCyclePhase(day) {
  if (day <= 5) return "Menstrual phase 🩸";
  if (day <= 14) return "Follicular phase 🌱";
  if (day <= 17) return "Ovulation phase 🌼";
  if (day <= 28) return "Luteal phase 🌙";
  return "New cycle approaching 🔄";
}

function updateUI() {
  const message = document.getElementById('message');
  if (!lastPeriod) {
    message.textContent = "Click the button to mark your last period start.";
    return;
  }
  const day = getCycleDay();
  const nextPeriod = calculateNextPeriod().toDateString();
  const phase = getCyclePhase(day);
  message.textContent = `Day ${day} — ${phase}. Next period: ${nextPeriod}`;
}

updateUI();

