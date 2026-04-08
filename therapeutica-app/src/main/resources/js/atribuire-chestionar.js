/**
 * Script pentru pagina de atribuire chestionar
 * Gestionează validarea și trimiterea formularului de atribuire
 */

// Funcție pentru a afișa/ascunde detaliile chestionarului
function toggleDetails(id) {
    const element = document.getElementById(id);
    if (element) {
        element.classList.toggle('hidden');

        // Schimbă și iconița chevron
        const button = document.querySelector(`button[onclick="toggleDetails('${id}')"]`);
        if (button) {
            const icon = button.querySelector('.fa-chevron-down, .fa-chevron-up');
            if (icon) {
                icon.classList.toggle('fa-chevron-down');
                icon.classList.toggle('fa-chevron-up');
            }
        }
    }
}

// Funcție pentru validare și trimitere formular
function submitAtribuire() {
    const form = document.getElementById('atribuireForm');
    const checkboxes = form.querySelectorAll('input[type="checkbox"][name="chestionareIds"]:checked');

    // Validare: cel puțin un chestionar selectat
    if (checkboxes.length === 0) {
        showAlert('error', 'Vă rugăm să selectați cel puțin un chestionar pentru atribuire!');
        return;
    }

    // Construiește mesajul de confirmare
    const numChestionare = checkboxes.length;
    const mesaj = `Sunteți sigur că doriți să atribuiți ${numChestionare} chestionar${numChestionare > 1 ? 'e' : ''} pacientului?`;

    // Confirmare
    if (confirm(mesaj)) {
        // Adaugă indicator de loading
        const submitButton = event.target;
        const originalHTML = submitButton.innerHTML;
        submitButton.disabled = true;
        submitButton.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i>Se atribuie...';

        // Trimite formularul
        form.submit();
    }
}

// Funcție pentru afișarea alertelor
function showAlert(type, message) {
    const alertDiv = document.createElement('div');
    alertDiv.className = `fixed top-4 right-4 z-50 p-4 rounded-lg shadow-lg max-w-md animate-slide-in ${
        type === 'error' ? 'bg-red-100 border border-red-400 text-red-700' :
        'bg-green-100 border border-green-400 text-green-700'
    }`;

    alertDiv.innerHTML = `
        <div class="flex items-center">
            <i class="fas ${type === 'error' ? 'fa-exclamation-triangle' : 'fa-check-circle'} mr-2"></i>
            <span>${message}</span>
            <button onclick="this.parentElement.parentElement.remove()" class="ml-4 text-lg font-bold">&times;</button>
        </div>
    `;

    document.body.appendChild(alertDiv);

    // Auto-remove după 5 secunde
    setTimeout(() => {
        alertDiv.remove();
    }, 5000);
}

// Contor pentru chestionare selectate
function updateSelectedCount() {
    const checkboxes = document.querySelectorAll('input[type="checkbox"][name="chestionareIds"]');
    const selected = Array.from(checkboxes).filter(cb => cb.checked).length;

    // Actualizează contorul dacă există
    const counter = document.getElementById('selectedCounter');
    if (counter) {
        counter.textContent = selected;
    }

    // Actualizează textul butonului de submit
    const submitButton = document.querySelector('button[onclick="submitAtribuire()"]');
    if (submitButton) {
        const buttonText = submitButton.querySelector('.button-text');
        if (buttonText) {
            buttonText.textContent = selected > 0
                ? `Confirmă atribuirea (${selected})`
                : 'Confirmă atribuirea';
        }
    }

    // Activează/dezactivează butonul
    if (submitButton) {
        submitButton.disabled = selected === 0;
        if (selected === 0) {
            submitButton.classList.add('opacity-50', 'cursor-not-allowed');
        } else {
            submitButton.classList.remove('opacity-50', 'cursor-not-allowed');
        }
    }
}

// Funcție pentru a evidenția chestionarul când se bifează checkbox-ul
function highlightChestionar(checkbox) {
    const container = checkbox.closest('.border');
    if (container) {
        if (checkbox.checked) {
            container.classList.add('border-blue-500', 'bg-blue-50', 'shadow-md');
            container.classList.remove('border-gray-200');
        } else {
            container.classList.remove('border-blue-500', 'bg-blue-50', 'shadow-md');
            container.classList.add('border-gray-200');
        }
    }
}

// Inițializare la încărcarea paginii
document.addEventListener('DOMContentLoaded', function() {
    console.log('✅ Script atribuire chestionar încărcat');

    // Adaugă event listener pentru checkbox-uri
    const checkboxes = document.querySelectorAll('input[type="checkbox"][name="chestionareIds"]');
    checkboxes.forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            updateSelectedCount();
            highlightChestionar(this);
        });
    });

    // Inițializează contorul și starea inițială
    updateSelectedCount();

    // Adaugă animație CSS pentru alerte (dacă nu există deja în Tailwind)
    if (!document.querySelector('#alert-animations')) {
        const style = document.createElement('style');
        style.id = 'alert-animations';
        style.textContent = `
            @keyframes slide-in {
                from {
                    transform: translateX(100%);
                    opacity: 0;
                }
                to {
                    transform: translateX(0);
                    opacity: 1;
                }
            }
            .animate-slide-in {
                animation: slide-in 0.3s ease-out;
            }
        `;
        document.head.appendChild(style);
    }

    console.log('📊 Chestionare disponibile găsite:', checkboxes.length);
});