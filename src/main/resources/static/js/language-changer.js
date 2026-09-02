/**
 * SmartCare 360 - Comprehensive Client-Side Patient Portal Language Engine
 * Supports Kannada (ಕನ್ನಡ), Hindi (हिन्दी), Spanish, French, German, Arabic, Tamil, and Telugu.
 * Features:
 *  - High-performance recursive TreeWalker DOM translation across all pages
 *  - Original text preservation (seamless switching between any language and English)
 *  - Dynamic Input placeholder translation
 *  - Google Translate fallback integration
 *  - LocalStorage & Cookie synchronization with backend PatientProfile
 */

const SMARTCARE_LANGUAGES = {
    'en': { name: 'English', nativeName: 'English', flag: '🇬🇧', dir: 'ltr' },
    'kn': { name: 'Kannada', nativeName: 'ಕನ್ನಡ (Kannada)', flag: '🇮🇳', dir: 'ltr' },
    'hi': { name: 'Hindi', nativeName: 'हिन्दी (Hindi)', flag: '🇮🇳', dir: 'ltr' },
    'es': { name: 'Spanish', nativeName: 'Español (Spanish)', flag: '🇪🇸', dir: 'ltr' },
    'fr': { name: 'French', nativeName: 'Français (French)', flag: '🇫🇷', dir: 'ltr' },
    'de': { name: 'German', nativeName: 'Deutsch (German)', flag: '🇩🇪', dir: 'ltr' },
    'ar': { name: 'Arabic', nativeName: 'العربية (Arabic)', flag: '🇸🇦', dir: 'rtl' },
    'ta': { name: 'Tamil', nativeName: 'தமிழ் (Tamil)', flag: '🇮🇳', dir: 'ltr' },
    'te': { name: 'Telugu', nativeName: 'తెలుగు (Telugu)', flag: '🇮🇳', dir: 'ltr' }
};

const I18N_DICTIONARY = {
    'kn': {
        // Sidebar Sections & Global Headers
        'OVERVIEW': 'ಅವಲೋಕನ',
        'CARE SERVICES': 'ಆರೈಕೆ ಸೇವೆಗಳು',
        'CLINICAL': 'ಕ್ಲಿನಿಕಲ್ ಸೇವೆಗಳು',
        'HEALTH & WELLNESS': 'ಆರೋಗ್ಯ ಮತ್ತು ಕ್ಷೇಮ',
        'PAYMENTS': 'ಪಾವತಿಗಳು',
        'ACCOUNT': 'ಖಾತೆ ವಿವರಗಳು',
        'SmartCare 360': 'ಸ್ಮಾರ್ಟ್‌ಕೇರ್ 360',
        'Portal': 'ಪೋರ್ಟಲ್',
        'Search patients, doctors, appointments, reports...': 'ರೋಗಿಗಳು, ವೈದ್ಯರು, ಅಪಾಯಿಂಟ್‌ಮೆಂಟ್‌ಗಳನ್ನು ಹುಡುಕಿ...',
        'Sign Out': 'ಸೈನ್ ಔಟ್ (ನಿರ್ಗಮಿಸಿ)',
        'Full Language Settings': 'ಸಂಪೂರ್ಣ ಭಾಷಾ ಸೆಟ್ಟಿಂಗ್‌ಗಳು',

        // Navigation Items
        'Dashboard': 'ಡ್ಯಾಶ್‌ಬೋರ್ಡ್',
        'Appointments': 'ಅಪಾಯಿಂಟ್‌ಮೆಂಟ್‌ಗಳು',
        'Live Queue': 'ಲೈವ್ ಸರತಿ ಸಾಲು',
        'Find Doctors': 'ವೈದ್ಯರನ್ನು ಹುಡುಕಿ',
        'Video Consultation': 'ವೀಡಿಯೊ ಸಮಾಲೋಚನೆ',
        'Prescriptions': 'ಔಷಧ ಚೀಟಿ (ಪ್ರಿಸ್ಕ್ರಿಪ್ಷನ್)',
        'Lab Reports': 'ಲ್ಯಾಬ್ ವರದಿಗಳು',
        'Medical Records': 'ವೈದ್ಯಕೀಯ ದಾಖಲೆಗಳು',
        'AI Health Suite': 'ಎಐ ಆರೋಗ್ಯ ಸೂಟ್',
        'Health Vitals': 'ಆರೋಗ್ಯ ಸೂಚಕಗಳು',
        'Pharmacy Orders': 'ಔಷಧಾಲಯ ಆರ್ಡರ್‌ಗಳು',
        'Buy Blood (Rx)': 'ರಕ್ತ ಖರೀದಿ (ಪ್ರಿಸ್ಕ್ರಿಪ್ಷನ್)',
        'Bed Booking': 'ಹಾಸಿಗೆ ಕಾಯ್ದಿರಿಸುವಿಕೆ',
        'Symptom Checker': 'ರೋಗಲಕ್ಷಣ ಪರೀಕ್ಷಕ',
        'Family Proxy': 'ಕುಟುಂಬ ಪ್ರತಿನಿಧಿ',
        'Health Library': 'ಆರೋಗ್ಯ ಗ್ರಂಥಾಲಯ',
        'Ambulance': 'ಆಂಬ್ಯುಲೆನ್ಸ್',
        'Bills & Payments': 'ಬಿಲ್ಲುಗಳು ಮತ್ತು ಪಾವತಿಗಳು',
        'Insurance Claims': 'ವಿಮಾ ಕ್ಲೈಮ್‌ಗಳು',
        'Multi-Hub Chat': 'ಮಲ್ಟಿ-ಹಬ್ ಚಾಟ್',
        'Notifications': 'ಸೂಚನೆಗಳು',
        'Profile': 'ಪ್ರೊಫೈಲ್',
        'Language / ಭಾಷೆ': 'ಭಾಷೆ / Language',

        // Dashboard & Cards
        'Welcome back': 'ಮರಳಿ ಸ್ವಾಗತ',
        'How can SmartCare 360 help you today?': 'ಸ್ಮಾರ್ಟ್‌ಕೇರ್ 360 ಇಂದು ನಿಮಗೆ ಹೇಗೆ ಸಹಾಯ ಮಾಡಬಹುದು?',
        'Book Appointment': 'ಅಪಾಯಿಂಟ್‌ಮೆಂಟ್ ಕಾಯ್ದಿರಿಸಿ',
        'Emergency Help': 'ತುರ್ತು ಸಹಾಯ',
        'Join Consultation': 'ಸಮಾಲೋಚನೆಗೆ ಸೇರಿ',
        'View Prescription': 'ಪ್ರಿಸ್ಕ್ರಿಪ್ಷನ್ ವೀಕ್ಷಿಸಿ',
        'View Reports': 'ವರದಿಗಳನ್ನು ವೀಕ್ಷಿಸಿ',
        'Pay Bills': 'ಬಿಲ್ಲುಗಳನ್ನು ಪಾವತಿಸಿ',
        'Upcoming Appointment': 'ಮುಂಬರುವ ಅಪಾಯಿಂಟ್‌ಮೆಂಟ್',
        'Confirmed': 'ದೃಢೀಕರಿಸಲಾಗಿದೆ',
        'Appointment History': 'ಅಪಾಯಿಂಟ್‌ಮೆಂಟ್ ಇತಿಹಾಸ',
        'Reason:': 'ಕಾರಣ:',
        'General Checkup': 'ಸಾಮಾನ್ಯ ತಪಾಸಣೆ',
        'View': 'ವೀಕ್ಷಿಸಿ',
        'Join Video': 'ವೀಡಿಯೊಗೆ ಸೇರಿ',
        'No upcoming appointments scheduled.': 'ಯಾವುದೇ ಮುಂಬರುವ ಅಪಾಯಿಂಟ್‌ಮೆಂಟ್‌ಗಳಿಲ್ಲ.',
        'Book one now': 'ಈಗಲೇ ಕಾಯ್ದಿರಿಸಿ',
        'Health Profile': 'ಆರೋಗ್ಯ ಪ್ರೊಫೈಲ್',
        'Blood Group': 'ರಕ್ತದ ಗುಂಪು',
        'Allergies': 'ಅಲರ್ಜಿಗಳು',
        'None recorded': 'ಯಾವುದೂ ದಾಖಲಾಗಿಲ್ಲ',
        'Recent Lab Reports': 'ಇತ್ತೀಚಿನ ಲ್ಯಾಬ್ ವರದಿಗಳು',
        'Active Prescriptions': 'ಸಕ್ರಿಯ ಪ್ರಿಸ್ಕ್ರಿಪ್ಷನ್‌ಗಳು',
        'Pending Payments': 'ಬಾಕಿ ಪಾವತಿಗಳು',

        // Common Actions & Form Controls
        'Doctor': 'ವೈದ್ಯರು',
        'Patient': 'ರೋಗಿ',
        'Patient Name': 'ರೋಗಿಯ ಹೆಸರು',
        'Doctor Name': 'ವೈದ್ಯರ ಹೆಸರು',
        'Specialty': 'ವಿಶೇಷತೆ',
        'Department': 'ವಿಭಾಗ',
        'Date': 'ದಿನಾಂಕ',
        'Time': 'ಸಮಯ',
        'Token': 'ಟೋಕನ್',
        'Room': 'ಕೊಠಡಿ',
        'Status': 'ಸ್ಥಿತಿ',
        'Action': 'ಕ್ರಮ',
        'Details': 'ವಿವರಗಳು',
        'Download': 'ಡೌನ್‌ಲೋಡ್',
        'Download PDF': 'ಪಿಡಿಎಫ್ ಡೌನ್‌ಲೋಡ್',
        'Pay Now': 'ಈಗಲೇ ಪಾವತಿಸಿ',
        'Cancel': 'ರದ್ದುಮಾಡಿ',
        'Submit': 'ಸಲ್ಲಿಸಿ',
        'Save': 'ಉಳಿಸಿ',
        'Update': 'ನವೀಕರಿಸಿ',
        'Filter': 'ಫಿಲ್ಟರ್',
        'Back': 'ಹಿಂದೆ',
        'Select': 'ಆಯ್ಕೆಮಾಡಿ',
        'Search': 'ಹುಡುಕಿ',
        'Blood Bank': 'ರಕ್ತ ನಿಧಿ (ಬ್ಲಡ್ ಬ್ಯಾಂಕ್)',
        'Buy Blood with Doctor Prescription': 'ವೈದ್ಯರ ಚೀಟಿಯೊಂದಿಗೆ ರಕ್ತ ಖರೀದಿಸಿ',
        'Available': 'ಲಭ್ಯವಿದೆ',
        'Units': 'ಯುನಿಟ್‌ಗಳು',
        'Active': 'ಸಕ್ರಿಯ',
        'Pending': 'ಬಾಕಿ ಇದೆ',
        'Completed': 'ಪೂರ್ಣಗೊಂಡಿದೆ',
        'Cancelled': 'ರದ್ದುಗೊಳಿಸಲಾಗಿದೆ',
        'Approved': 'ಅನುಮೋದಿಸಲಾಗಿದೆ',
        'Rejected': 'ತಿರಸ್ಕರಿಸಲಾಗಿದೆ',
        'Settled': 'ಪಾವತಿಸಲಾಗಿದೆ'
    },
    'hi': {
        'OVERVIEW': 'अवलोकन',
        'CARE SERVICES': 'चिकित्सा सेवाएं',
        'CLINICAL': 'क्लिनिकल',
        'PAYMENTS': 'भुगतान',
        'ACCOUNT': 'खाता',
        'Dashboard': 'डैशबोर्ड',
        'Appointments': 'अपॉइंटमेंट्स',
        'Live Queue': 'लाइव कतार',
        'Find Doctors': 'डॉक्टर खोजें',
        'Video Consultation': 'वीडियो परामर्श',
        'Prescriptions': 'नुस्खे (प्रिस्क्रिप्शन)',
        'Lab Reports': 'लैब रिपोर्ट',
        'Medical Records': 'चिकित्सा रिकॉर्ड',
        'AI Health Suite': 'एआई स्वास्थ्य सुइट',
        'Health Vitals': 'स्वास्थ्य वाइटल्स',
        'Pharmacy Orders': 'दवाइयों के ऑर्डर',
        'Buy Blood (Rx)': 'ब्लड खरीदें (प्रिस्क्रिप्शन)',
        'Bed Booking': 'बेड बुकिंग',
        'Bills & Payments': 'बिल और भुगतान',
        'Insurance Claims': 'बीमा दावे',
        'Multi-Hub Chat': 'मल्टी-हब चैट',
        'Notifications': 'सूचनाएं',
        'Profile': 'प्रोफ़ाइल',
        'Sign Out': 'साइन आउट',
        'Book Appointment': 'अपॉइंटमेंट बुक करें',
        'Emergency Help': 'आपातकालीन सहायता',
        'Doctor': 'डॉक्टर',
        'Patient': 'मरीज़',
        'Blood Bank': 'ब्लड बैंक',
        'Pay Now': 'अभी भुगतान करें'
    },
    'es': {
        'OVERVIEW': 'RESUMEN',
        'CARE SERVICES': 'SERVICIOS DE ATENCIÓN',
        'PAYMENTS': 'PAGOS',
        'ACCOUNT': 'CUENTA',
        'Dashboard': 'Panel Principal',
        'Appointments': 'Citas Médicas',
        'Live Queue': 'Cola en Vivo',
        'Prescriptions': 'Recetas Médicas',
        'Lab Reports': 'Informes de Laboratorio',
        'Medical Records': 'Historial Médico',
        'Find Doctors': 'Buscar Médicos',
        'Video Consultation': 'Videoconsulta',
        'Pharmacy Orders': 'Pedidos de Farmacia',
        'Buy Blood (Rx)': 'Comprar Sangre (Receta)',
        'Bed Booking': 'Reserva de Camas',
        'Bills & Payments': 'Facturas y Pagos',
        'Insurance Claims': 'Reclamaciones de Seguro',
        'Notifications': 'Notificaciones',
        'Sign Out': 'Cerrar Sesión',
        'Book Appointment': 'Reservar Cita',
        'Pay Now': 'Pagar Ahora'
    }
};

function getCurrentLanguage() {
    return localStorage.getItem('smartcare_preferred_language') || 'en';
}

function toggleLanguageDropdown() {
    const menu = document.getElementById('languageDropdownMenu');
    if (menu) {
        menu.style.display = (menu.style.display === 'block') ? 'none' : 'block';
    }
}

// Close dropdown when clicking outside
document.addEventListener('click', function(e) {
    const container = document.querySelector('.lang-selector-container');
    const menu = document.getElementById('languageDropdownMenu');
    if (container && menu && !container.contains(e.target)) {
        menu.style.display = 'none';
    }
});

function switchLanguage(langCode, langName) {
    if (!SMARTCARE_LANGUAGES[langCode]) {
        langCode = 'en';
    }

    localStorage.setItem('smartcare_preferred_language', langCode);

    // Set standard cookies
    document.cookie = `smartcare_lang=${langCode}; path=/; max-age=31536000`;
    document.cookie = `googtrans=/en/${langCode}; path=/;`;
    document.cookie = `googtrans=/auto/${langCode}; path=/;`;

    // Apply immediate client-side DOM translation
    applyLanguageToDOM(langCode);

    // Update Header label
    const labelEl = document.getElementById('currentLangLabel');
    if (labelEl) {
        labelEl.innerText = SMARTCARE_LANGUAGES[langCode].name;
    }

    // Update active badge if on language settings page
    if (typeof highlightCard === 'function') {
        highlightCard(langCode);
    }

    // Close menu
    const menu = document.getElementById('languageDropdownMenu');
    if (menu) menu.style.display = 'none';

    // Show Language Floating Badge
    updateLanguageFloatingBadge(langCode);

    // Notify backend asynchronously
    fetch('/patient/api/language', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: 'lang=' + encodeURIComponent(langCode)
    }).catch(err => console.log('Language sync warning', err));

    // Also trigger Google Translate if widget element is present
    const select = document.querySelector('.goog-te-combo');
    if (select) {
        select.value = langCode;
        select.dispatchEvent(new Event('change'));
    }
}

/**
 * High performance recursive TreeWalker to translate text nodes in DOM
 */
function applyLanguageToDOM(langCode) {
    const langMeta = SMARTCARE_LANGUAGES[langCode] || SMARTCARE_LANGUAGES['en'];
    document.documentElement.lang = langCode;
    document.documentElement.dir = langMeta.dir || 'ltr';

    const dict = I18N_DICTIONARY[langCode];

    const walker = document.createTreeWalker(
        document.body,
        NodeFilter.SHOW_TEXT,
        {
            acceptNode: function(node) {
                if (node.parentNode) {
                    const tag = node.parentNode.tagName;
                    if (tag === 'SCRIPT' || tag === 'STYLE' || tag === 'NOSCRIPT' || tag === 'TEXTAREA') {
                        return NodeFilter.FILTER_REJECT;
                    }
                    if (node.parentNode.classList && (node.parentNode.classList.contains('goog-te-banner') || node.parentNode.classList.contains('notranslate'))) {
                        return NodeFilter.FILTER_REJECT;
                    }
                }
                return NodeFilter.FILTER_ACCEPT;
            }
        }
    );

    let node;
    while ((node = walker.nextNode())) {
        if (!node._smartcareOrig) {
            node._smartcareOrig = node.nodeValue;
        }

        if (langCode === 'en' || !dict) {
            node.nodeValue = node._smartcareOrig;
            continue;
        }

        let val = node._smartcareOrig;
        const trimmed = val.trim();

        if (dict[trimmed]) {
            node.nodeValue = val.replace(trimmed, dict[trimmed]);
        } else {
            // Check keys in dictionary sorted by length desc for phrase replacement
            for (const key of Object.keys(dict)) {
                if (key.length > 3 && val.includes(key)) {
                    val = val.replaceAll(key, dict[key]);
                }
            }
            node.nodeValue = val;
        }
    }

    // Translate input placeholders
    document.querySelectorAll('input[placeholder]').forEach(input => {
        if (!input.dataset.origPlaceholder) {
            input.dataset.origPlaceholder = input.placeholder;
        }
        if (langCode === 'en' || !dict) {
            input.placeholder = input.dataset.origPlaceholder;
        } else if (dict[input.dataset.origPlaceholder.trim()]) {
            input.placeholder = dict[input.dataset.origPlaceholder.trim()];
        }
    });

    updateLanguageFloatingBadge(langCode);
}

function updateLanguageFloatingBadge(langCode) {
    let badge = document.getElementById('smartcare-lang-floating-badge');
    if (langCode === 'en') {
        if (badge) badge.style.display = 'none';
        return;
    }

    const langInfo = SMARTCARE_LANGUAGES[langCode];
    if (!badge) {
        badge = document.createElement('div');
        badge.id = 'smartcare-lang-floating-badge';
        badge.style.cssText = 'position:fixed;bottom:20px;left:20px;z-index:99999;background:#0f172a;color:#fff;padding:8px 14px;border-radius:24px;box-shadow:0 6px 20px rgba(0,0,0,0.25);display:flex;align-items:center;gap:8px;font-size:0.82rem;font-weight:600;border:1px solid #334155;';
        document.body.appendChild(badge);
    }
    badge.style.display = 'flex';
    badge.innerHTML = `<span>🌐</span> <span>${langInfo.nativeName} Active</span> <a href="/patient/language" style="color:#60a5fa;text-decoration:none;margin-left:6px;font-size:0.75rem;padding:2px 6px;background:#1e293b;border-radius:12px;">Change</a>`;
}

// Google Translate fallback hook
function googleTranslateElementInit() {
    new google.translate.TranslateElement({
        pageLanguage: 'en',
        includedLanguages: 'en,kn,hi,es,fr,de,ar,ta,te',
        autoDisplay: false
    }, 'google_translate_element');
}

// Auto-run on page load
document.addEventListener('DOMContentLoaded', () => {
    const savedLang = getCurrentLanguage();

    const labelEl = document.getElementById('currentLangLabel');
    if (labelEl && SMARTCARE_LANGUAGES[savedLang]) {
        labelEl.innerText = SMARTCARE_LANGUAGES[savedLang].name;
    }

    if (savedLang !== 'en') {
        applyLanguageToDOM(savedLang);
    }

    // Dynamically mount Google translate element
    if (!document.getElementById('google-translate-script')) {
        const div = document.createElement('div');
        div.id = 'google_translate_element';
        div.style.display = 'none';
        document.body.appendChild(div);

        window.googleTranslateElementInit = googleTranslateElementInit;

        const script = document.createElement('script');
        script.id = 'google-translate-script';
        script.src = '//translate.google.com/translate_a/element.js?cb=googleTranslateElementInit';
        script.async = true;
        document.body.appendChild(script);
    }
});
