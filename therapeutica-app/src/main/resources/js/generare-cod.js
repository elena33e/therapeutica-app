const API_BASE = 'http://localhost:8080/api';

        // Obține userId din URL sau session
        function getUserId() {

            return localStorage.getItem('userId') || sessionStorage.getItem('userId');
        }

        // Obține token
        function getToken() {
            return localStorage.getItem('accessToken') || sessionStorage.getItem('accessToken');
        }

        // Verifică autentificarea
        const token = getToken();
        const userId = getUserId();

        if (!token || !userId) {
            console.warn('User not authenticated, redirecting to login');
            window.location.href = '/login?redirect=/generare-cod';
        }

        document.addEventListener('DOMContentLoaded', function() {
            console.log('Generare cod page loaded');
            console.log('User ID:', userId);
            incarcaCodurileMele();
        });

        // Formular generare cod
        document.getElementById('formGenerareCod').addEventListener('submit', async (e) => {
            e.preventDefault();

            const emailDestinatar = document.getElementById('emailDestinatar').value.trim();
            const cnpDestinatar = document.getElementById('cnpDestinatar').value.trim();
            const rolDestinatar = document.getElementById('rolDestinatar').value;
            const numeDestinatar = document.getElementById('numeDestinatar').value.trim();
const prenumeDestinatar = document.getElementById('prenumeDestinatar').value.trim();

            // Validări
            if (!emailDestinatar || !rolDestinatar) {
                alert('Completează toate câmpurile obligatorii!');
                return;
            }

            if (cnpDestinatar && !/^\d{13}$/.test(cnpDestinatar)) {
                alert('CNP-ul trebuie să conțină exact 13 cifre!');
                return;
            }

            // Pregătește request
            const requestData = {
    medicId: userId,
    emailDestinatar: emailDestinatar,
    cnpDestinatar: cnpDestinatar || null,
    rolDestinatar: rolDestinatar,
    numeDestinatar: numeDestinatar,    // ← NOU
    prenumeDestinatar: prenumeDestinatar  // ← NOU
};

            console.log('Generare cod request:', requestData);

            // Loading state
            const submitBtn = document.getElementById('submitBtn');
            const btnText = document.getElementById('btnText');
            const btnSpinner = document.getElementById('btnSpinner');

            btnText.textContent = 'Se generează...';
            btnSpinner.classList.remove('hidden');
            submitBtn.disabled = true;

            try {
                const response = await fetch(`${API_BASE}/coduri-inregistrare/generare`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${token}`
                    },
                    body: JSON.stringify(requestData)
                });

                const data = await response.json();
                console.log('Response:', data);

                if (response.ok) {
                    // Afișează codul generat
                    document.getElementById('codGeneratAfisare').textContent = data.codUnic;
                    document.getElementById('emailAfisat').textContent = emailDestinatar;
                    document.getElementById('rolAfisat').textContent = rolDestinatar === 'PACIENT' ? 'Pacient' : 'Medic';

                    // Show success message
                    const rezultatDiv = document.getElementById('rezultatCod');
                    rezultatDiv.classList.remove('hidden');

                    // Reset form
                    document.getElementById('formGenerareCod').reset();

                    // Scroll la rezultat
                    setTimeout(() => {
                        rezultatDiv.scrollIntoView({ behavior: 'smooth' });
                    }, 100);

                    // Reîncarcă lista
                    setTimeout(incarcaCodurileMele, 500);

                } else {
                    alert('Eroare: ' + (data.mesaj || 'A apărut o eroare la generare'));
                }
            } catch (error) {
                console.error('Error:', error);
                alert('Eroare de conexiune la server');
            } finally {
                // Reset button
                btnText.textContent = 'Generează Cod';
                btnSpinner.classList.add('hidden');
                submitBtn.disabled = false;
            }
        });

        // Funcție pentru copiere cod în clipboard
        function copyToClipboard() {
            const cod = document.getElementById('codGeneratAfisare').textContent;
            navigator.clipboard.writeText(cod).then(() => {
                // Feedback vizual
                const btn = document.querySelector('[onclick="copyToClipboard()"]');
                const originalHtml = btn.innerHTML;
                btn.innerHTML = '<i class="fas fa-check mr-1"></i> Copiat!';
                btn.classList.add('text-green-600');

                setTimeout(() => {
                    btn.innerHTML = originalHtml;
                    btn.classList.remove('text-green-600');
                }, 2000);
            });
        }

        // Funcție pentru încărcarea codurilor
        async function incarcaCodurileMele() {
            const container = document.getElementById('listaCoduri');
            const loading = document.getElementById('loadingCoduri');

            // Show loading
            container.innerHTML = '';
            loading.classList.remove('hidden');

            try {
                const response = await fetch(`${API_BASE}/coduri-inregistrare/medic/${userId}`, {
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });

                if (response.ok) {
                    const coduri = await response.json();
                    console.log('Coduri loaded:', coduri);
                    afiseazaCodurile(coduri);
                    actualizeazaStatistici(coduri);
                } else {
                    console.error('Failed to load coduri:', response.status);
                    container.innerHTML = `
                        <div class="text-center py-8 text-yellow-500 bg-white rounded-lg border border-yellow-200">
                            <i class="fas fa-exclamation-triangle text-2xl mb-3"></i>
                            <p>Nu s-au putut încărca codurile.</p>
                        </div>`;
                }
            } catch (error) {
                console.error('Eroare încărcare coduri:', error);
                container.innerHTML = `
                    <div class="text-center py-8 text-red-500 bg-white rounded-lg border border-red-200">
                        <i class="fas fa-exclamation-circle text-2xl mb-3"></i>
                        <p>Eroare la încărcarea codurilor.</p>
                        <button onclick="incarcaCodurileMele()" class="text-blue-600 mt-2 hover:underline">
                            Reîncearcă
                        </button>
                    </div>`;
            } finally {
                loading.classList.add('hidden');
            }
        }

        // Funcție pentru afișarea codurilor
        function afiseazaCodurile(coduri) {
            const container = document.getElementById('listaCoduri');

            if (!coduri || coduri.length === 0) {
                container.innerHTML = `
                    <div class="text-center py-8 text-gray-500 bg-white rounded-lg border border-gray-200">
                        <i class="fas fa-key text-3xl mb-3 text-gray-300"></i>
                        <p class="text-gray-600">Nu ai generat încă niciun cod.</p>
                        <p class="text-sm text-gray-500 mt-1">Folosește formularul de mai sus pentru a genera primul cod.</p>
                    </div>`;
                return;
            }

            container.innerHTML = '';

            coduri.forEach(cod => {
                const div = document.createElement('div');
                div.className = 'bg-white p-5 rounded-lg shadow-sm border border-gray-200 hover:border-blue-300 transition';

                // Formatează data
                const data = cod.createdAt ? new Date(cod.createdAt).toLocaleDateString('ro-RO', {
                    day: '2-digit',
                    month: '2-digit',
                    year: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit'
                }) : 'N/A';

                // Status badge
                const statusClass = cod.status === 'NEUTILIZAT' ? 'status-neutilizat' : 'status-utilizat';
                const statusText = cod.status === 'NEUTILIZAT' ? 'Activ' : 'Utilizat';

                div.innerHTML = `
                    <div class="flex flex-col md:flex-row md:items-center justify-between">
                        <div class="mb-4 md:mb-0">
                            <div class="flex items-center mb-2">
                                <span class="font-mono text-xl font-bold text-blue-700 bg-blue-50 px-3 py-1 rounded">${cod.codUnic}</span>
                                <span class="status-badge ${statusClass} ml-3">${statusText}</span>
                            </div>
                            <div class="text-sm text-gray-600 space-y-1">
                                <p><i class="fas fa-user mr-2"></i>Destinatar: <span class="font-medium">${cod.emailDestinatar}</span></p>
                                <p><i class="fas fa-tag mr-2"></i>Rol: <span class="font-medium">${cod.rolDestinatar}</span></p>
                                ${cod.cnpDestinatar ? `<p><i class="fas fa-id-card mr-2"></i>CNP: <span class="font-mono">${cod.cnpDestinatar}</span></p>` : ''}
                            </div>
                        </div>
                        <div class="text-right">
                            <p class="text-sm text-gray-500 mb-2">
                                <i class="fas fa-calendar mr-1"></i> ${data}
                            </p>
                        </div>
                    </div>
                `;

                container.appendChild(div);
            });
        }

        // Actualizează statisticile
        function actualizeazaStatistici(coduri) {
            const total = coduri.length;
            const active = coduri.filter(c => c.status === 'NEUTILIZAT').length;
            const utilizate = total - active;

            document.getElementById('totalCoduri').textContent = total;
            document.getElementById('coduriActive').textContent = active;
            document.getElementById('coduriUtilizate').textContent = utilizate;
        }