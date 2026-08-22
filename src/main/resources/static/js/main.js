// SmartCare 360 — Portal navigation & mobile UX
document.addEventListener('DOMContentLoaded', () => {
    initPortalDrawers();
    initBottomNavActive();
    initResponsiveTables();
});

function initPortalDrawers() {
    const openButtons = document.querySelectorAll('[data-drawer-open]');
    const closeButtons = document.querySelectorAll('[data-drawer-close]');
    const overlays = document.querySelectorAll('.portal-drawer-overlay');

    openButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const id = btn.getAttribute('data-drawer-open');
            const drawer = document.getElementById(id);
            if (drawer) {
                drawer.classList.add('open');
                document.body.classList.add('drawer-open');
            }
        });
    });

    const closeDrawer = (drawer) => {
        drawer.classList.remove('open');
        document.body.classList.remove('drawer-open');
    };

    closeButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const drawer = btn.closest('.portal-drawer');
            if (drawer) closeDrawer(drawer);
        });
    });

    overlays.forEach(overlay => {
        overlay.addEventListener('click', () => {
            const drawer = overlay.nextElementSibling;
            if (drawer && drawer.classList.contains('portal-drawer')) {
                closeDrawer(drawer);
            }
        });
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            document.querySelectorAll('.portal-drawer.open').forEach(closeDrawer);
        }
    });
}

function initBottomNavActive() {
    const path = window.location.pathname;
    document.querySelectorAll('.portal-bottom-nav a').forEach(link => {
        const href = link.getAttribute('href');
        if (!href || href === '#') return;
        if (path === href || (href !== '/' && path.startsWith(href))) {
            link.classList.add('active');
        }
    });
}

function initResponsiveTables() {
    document.querySelectorAll('.responsive-table').forEach(table => {
        const headers = Array.from(table.querySelectorAll('thead th')).map(th => th.textContent.trim());
        table.querySelectorAll('tbody tr').forEach(row => {
            row.querySelectorAll('td').forEach((cell, i) => {
                if (headers[i]) cell.setAttribute('data-label', headers[i]);
            });
        });
    });
}
