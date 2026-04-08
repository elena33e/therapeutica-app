// resources/static/js/medic-profil.js
document.addEventListener('DOMContentLoaded', function() {
    console.log('Medic profil page loaded');

    // Validare email în timp real
    const emailInput = document.querySelector('input[type="email"]');
    if (emailInput) {
        emailInput.addEventListener('blur', function() {
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            const errorElement = this.parentElement.querySelector('.text-red-500');

            if (this.value && !emailRegex.test(this.value)) {
                this.classList.add('border-red-500', 'ring-2', 'ring-red-200');
                if (errorElement) {
                    errorElement.textContent = 'Format email invalid';
                    errorElement.parentElement.classList.remove('hidden');
                }
            } else {
                this.classList.remove('border-red-500', 'ring-2', 'ring-red-200');
                if (errorElement) {
                    errorElement.textContent = '';
                    errorElement.parentElement.classList.add('hidden');
                }
            }
        });
    }

    // Confirmare înainte de anulare
    const anulareBtn = document.querySelector('a[href*="/medic/profil/"]:not([href*="/editare"])');
    if (anulareBtn && document.getElementById('profilForm')) {
        anulareBtn.addEventListener('click', function(e) {
            const form = document.getElementById('profilForm');
            if (form) {
                const inputs = form.querySelectorAll('input[type="text"], input[type="email"]');
                let hasChanges = false;

                inputs.forEach(input => {
                    const originalValue = input.defaultValue || '';
                    const currentValue = input.value;
                    if (originalValue !== currentValue) {
                        hasChanges = true;
                    }
                });

                if (hasChanges) {
                    if (!confirm('Ai modificări nesalvate. Ești sigur că vrei să anulezi?')) {
                        e.preventDefault();
                    }
                }
            }
        });
    }

    // Validare formular la submit
    const form = document.getElementById('profilForm');
    if (form) {
        form.addEventListener('submit', function(e) {
            let isValid = true;

            const requiredFields = form.querySelectorAll('[required]');
            requiredFields.forEach(field => {
                if (!field.value.trim()) {
                    field.classList.add('border-red-500', 'ring-2', 'ring-red-200');
                    isValid = false;

                    let errorDiv = field.parentElement.querySelector('.text-red-500');
                    if (!errorDiv) {
                        errorDiv = document.createElement('div');
                        errorDiv.className = 'mt-2 text-red-500 text-sm';
                        errorDiv.innerHTML = '<i class="fas fa-exclamation-circle mr-1"></i>Acest câmp este obligatoriu';
                        field.parentElement.appendChild(errorDiv);
                    }
                }
            });

            if (!isValid) {
                e.preventDefault();
                const firstError = form.querySelector('.border-red-500');
                if (firstError) {
                    firstError.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    firstError.focus();
                }
            }
        });
    }
});