// Helpify presentation behavior. Application routes, forms and server-side logic remain unchanged.
(() => {
  'use strict';

  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
  const finePointer = window.matchMedia('(hover: hover) and (pointer: fine)');

  // Password visibility.
  document.querySelectorAll('[data-toggle-password]').forEach(button => {
    button.addEventListener('click', () => {
      const input = document.getElementById(button.dataset.togglePassword);
      if (!input) return;
      const hidden = input.type === 'password';
      input.type = hidden ? 'text' : 'password';
      button.textContent = hidden ? 'Hide' : 'Show';
    });
  });

  // FAQ accordion.
  document.querySelectorAll('[data-accordion]').forEach(button => {
    button.addEventListener('click', () => {
      const card = button.closest('.faq-card');
      if (!card) return;
      card.classList.toggle('open');
      const icon = button.querySelector('i');
      if (icon) icon.textContent = card.classList.contains('open') ? '−' : '+';
      button.setAttribute('aria-expanded', String(card.classList.contains('open')));
    });
  });

  // Existing confirmation behavior.
  document.querySelectorAll('[data-confirm]').forEach(form => {
    form.addEventListener('submit', event => {
      if (!confirm(form.dataset.confirm)) event.preventDefault();
    });
  });

  // Responsive sidebar.
  const menu = document.querySelector('[data-menu]');
  const sidebar = document.getElementById('sidebar');
  if (menu && sidebar) {
    menu.addEventListener('click', event => {
      event.stopPropagation();
      sidebar.classList.toggle('open');
      menu.setAttribute('aria-expanded', String(sidebar.classList.contains('open')));
    });
    document.addEventListener('click', event => {
      if (innerWidth <= 1050 && !sidebar.contains(event.target) && !menu.contains(event.target)) {
        sidebar.classList.remove('open');
        menu.setAttribute('aria-expanded', 'false');
      }
    });
    window.addEventListener('resize', () => {
      if (innerWidth > 1050) {
        sidebar.classList.remove('open');
        menu.setAttribute('aria-expanded', 'false');
      }
    });
  }

  // Attachment preview. Server-side validation stays authoritative.
  document.querySelectorAll('[data-attachments]').forEach(input => {
    const output = input.closest('label')?.querySelector('.selected-files');
    let urls = [];

    const validate = () => {
      const files = [...input.files];
      let error = '';
      const maxFiles = Number(input.dataset.maxFiles || 5);
      const maxBytes = Number(input.dataset.maxBytes || 10485760);
      if (files.length > maxFiles) error = `Select at most ${maxFiles} files.`;
      else if (files.some(file => !file.size || file.size > maxBytes)) error = 'Each file must be non-empty and at most 10 MB.';
      else if (files.some(file => !(/\.(png|jpe?g|pdf|docx?)$/i.test(file.name)))) error = 'Use PNG, JPG, PDF, DOC or DOCX.';
      input.setCustomValidity(error);
      return !error;
    };

    const render = () => {
      urls.forEach(URL.revokeObjectURL);
      urls = [];
      if (!output) return;
      output.replaceChildren();
      [...input.files].forEach((file, index) => {
        const row = document.createElement('span');
        row.className = 'file-preview';
        if (['image/png', 'image/jpeg'].includes(file.type)) {
          const img = document.createElement('img');
          const url = URL.createObjectURL(file);
          urls.push(url);
          img.src = url;
          img.alt = 'Selected image';
          row.append(img);
        }
        const text = document.createElement('span');
        text.textContent = `${file.name} · ${(file.size / 1024 / 1024).toFixed(2)} MB`;
        row.append(text);
        const remove = document.createElement('button');
        remove.type = 'button';
        remove.textContent = 'Remove';
        remove.setAttribute('aria-label', `Remove ${file.name}`);
        remove.addEventListener('click', () => {
          const transfer = new DataTransfer();
          [...input.files].filter((_, i) => i !== index).forEach(fileItem => transfer.items.add(fileItem));
          input.files = transfer.files;
          validate();
          render();
        });
        row.append(remove);
        output.append(row);
      });
    };

    input.addEventListener('change', () => {
      validate();
      render();
      if (!input.validity.valid) input.reportValidity();
    });
  });

  // Validation and submit/loading states.
  document.querySelectorAll('form').forEach(form => {
    form.addEventListener('invalid', event => {
      const field = event.target;
      const wrapper = field.parentElement;
      if (!wrapper) return;
      let note = wrapper.querySelector('.field-error');
      if (!note) {
        note = document.createElement('span');
        note.className = 'field-error';
        wrapper.append(note);
      }
      note.textContent = field.validationMessage;
      field.setAttribute('aria-invalid', 'true');
    }, true);

    form.addEventListener('input', event => {
      event.target.removeAttribute('aria-invalid');
      const note = event.target.parentElement?.querySelector('.field-error');
      if (note) note.remove();
    });

    form.addEventListener('submit', event => {
      if (event.defaultPrevented) return;
      const button = event.submitter;
      if (button) {
        requestAnimationFrame(() => {
          button.disabled = true;
          button.setAttribute('aria-busy', 'true');
          const isLogin = form.action && form.action.includes('/login');
          if (isLogin && !reducedMotion.matches) document.body.classList.add('page-leaving');
        });
        setTimeout(() => {
          button.disabled = false;
          button.removeAttribute('aria-busy');
        }, 12000);
      }
    });
  });

  window.addEventListener('pageshow', () => {
    document.body.classList.remove('page-leaving');
    document.querySelectorAll('[aria-busy]').forEach(button => {
      button.disabled = false;
      button.removeAttribute('aria-busy');
    });
  });

  // Stagger rows without changing any data.
  document.querySelectorAll('tbody').forEach(body => {
    [...body.querySelectorAll(':scope > tr')].forEach((row, index) => {
      if (index > 11 || reducedMotion.matches) return;
      row.style.opacity = '0';
      row.style.transform = 'translateY(8px)';
      row.style.transition = `opacity .4s ${Math.min(index * 35, 280)}ms ease, transform .4s ${Math.min(index * 35, 280)}ms ease, background .18s`;
      requestAnimationFrame(() => requestAnimationFrame(() => {
        row.style.opacity = '1';
        row.style.transform = 'none';
      }));
    });
  });

  // Animated dashboard/report counters. Text/data are restored exactly at the end.
  const animateNumber = element => {
    if (reducedMotion.matches || element.dataset.counted === '1') return;
    const raw = element.textContent.trim();
    const normalized = raw.replace(/,/g, '');
    if (!/^\d+(\.\d+)?$/.test(normalized)) return;
    const target = Number(normalized);
    if (!Number.isFinite(target)) return;
    element.dataset.counted = '1';
    const decimals = normalized.includes('.') ? normalized.split('.')[1].length : 0;
    const duration = 760;
    const start = performance.now();
    const tick = now => {
      const progress = Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - progress, 3);
      const current = target * eased;
      element.textContent = decimals ? current.toFixed(decimals) : Math.round(current).toLocaleString();
      if (progress < 1) requestAnimationFrame(tick);
      else element.textContent = raw;
    };
    requestAnimationFrame(tick);
  };

  const counters = document.querySelectorAll('.stat-card > b, .pulse-ring b');
  if ('IntersectionObserver' in window && !reducedMotion.matches) {
    const countObserver = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          animateNumber(entry.target);
          countObserver.unobserve(entry.target);
        }
      });
    }, { threshold: .5 });
    counters.forEach(counter => countObserver.observe(counter));
  } else {
    counters.forEach(animateNumber);
  }

  // Scroll reveals for content below the first screen.
  const revealItems = document.querySelectorAll(
    '.feature-strip article, .dashboard-grid > .panel, .quick-grid > a, .faq-card, .profile-sections > .panel, .ticket-side > .panel, .feedback-card, .notification, .reports-grid > .panel, .timeline article'
  );
  let revealObserver;
  if ('IntersectionObserver' in window && !reducedMotion.matches) {
    revealObserver = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.remove('motion-pending');
          entry.target.classList.add('motion-visible');
          revealObserver.unobserve(entry.target);
        }
      });
    }, { threshold: .08 });
    revealItems.forEach((item, index) => {
      if (item.getBoundingClientRect().top > innerHeight * .88) {
        item.style.setProperty('--reveal-delay', `${(index % 4) * 60}ms`);
        item.classList.add('motion-ready', 'motion-pending');
        revealObserver.observe(item);
      }
    });
  }

  document.addEventListener('focusin', event => {
    const pending = event.target.closest('.motion-pending');
    if (pending) pending.classList.remove('motion-pending');
  });

  // Subtle pointer spotlight on interactive cards.
  const cards = document.querySelectorAll('.feature-strip article, .stat-card, .quick-grid > a, .faq-card, .help-card, .feedback-card, .panel');
  cards.forEach(card => {
    card.classList.add('spotlight-card');
    let frame = 0;
    card.addEventListener('pointermove', event => {
      if (reducedMotion.matches || !finePointer.matches || frame) return;
      frame = requestAnimationFrame(() => {
        const bounds = card.getBoundingClientRect();
        card.style.setProperty('--pointer-x', `${event.clientX - bounds.left}px`);
        card.style.setProperty('--pointer-y', `${event.clientY - bounds.top}px`);
        frame = 0;
      });
    });
    card.addEventListener('pointerleave', () => {
      cancelAnimationFrame(frame);
      frame = 0;
      card.style.removeProperty('--pointer-x');
      card.style.removeProperty('--pointer-y');
    });
  });

  // Button ripple.
  document.addEventListener('click', event => {
    const button = event.target.closest('.btn');
    if (!button || button.disabled || reducedMotion.matches) return;
    const bounds = button.getBoundingClientRect();
    const ripple = document.createElement('span');
    ripple.className = 'click-ripple';
    ripple.setAttribute('aria-hidden', 'true');
    ripple.style.setProperty('--ripple-x', `${event.detail ? event.clientX - bounds.left : bounds.width / 2}px`);
    ripple.style.setProperty('--ripple-y', `${event.detail ? event.clientY - bounds.top : bounds.height / 2}px`);
    button.append(ripple);
    ripple.addEventListener('animationend', () => ripple.remove(), { once: true });
    setTimeout(() => ripple.remove(), 900);
  });

  // Same-origin navigation fallback transition for browsers without cross-document view transitions.
  if (!reducedMotion.matches) {
    document.addEventListener('click', event => {
      const link = event.target.closest('a[href]');
      if (!link || event.defaultPrevented || event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
      if (link.target || link.hasAttribute('download')) return;
      const href = link.getAttribute('href');
      if (!href || href.startsWith('#') || href.startsWith('mailto:') || href.startsWith('tel:') || href.startsWith('javascript:')) return;
      let url;
      try { url = new URL(link.href, location.href); } catch { return; }
      if (url.origin !== location.origin || url.href === location.href) return;
      if (document.startViewTransition) return; // Native cross-document transition handles it.
      event.preventDefault();
      document.body.classList.add('page-leaving');
      setTimeout(() => { location.href = url.href; }, 175);
    });
  }

  reducedMotion.addEventListener?.('change', () => {
    if (!reducedMotion.matches) return;
    revealObserver?.disconnect();
    document.querySelectorAll('.motion-pending').forEach(item => item.classList.remove('motion-pending'));
  });
})();
