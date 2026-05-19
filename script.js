/* ==========================================
   GitHub Store — Landing Page Scripts
   ========================================== */

(function() {
  'use strict';

  // ---- Navbar scroll effect ----
  const navbar = document.getElementById('navbar');
  let lastScroll = 0;

  function handleScroll() {
    const scrollY = window.scrollY;
    if (scrollY > 50) {
      navbar.classList.add('scrolled');
    } else {
      navbar.classList.remove('scrolled');
    }
    lastScroll = scrollY;
  }

  window.addEventListener('scroll', handleScroll, { passive: true });
  handleScroll();

  // ---- Mobile menu toggle ----
  const navBurger = document.getElementById('navBurger');
  const mobileMenu = document.getElementById('mobileMenu');

  navBurger.addEventListener('click', function() {
    navBurger.classList.toggle('active');
    mobileMenu.classList.toggle('open');
    document.body.style.overflow = mobileMenu.classList.contains('open') ? 'hidden' : '';
  });

  // Close mobile menu on link click
  document.querySelectorAll('.mobile-link').forEach(function(link) {
    link.addEventListener('click', function() {
      navBurger.classList.remove('active');
      mobileMenu.classList.remove('open');
      document.body.style.overflow = '';
    });
  });

  // ---- Smooth scroll for anchor links ----
  document.querySelectorAll('a[href^="#"]').forEach(function(anchor) {
    anchor.addEventListener('click', function(e) {
      const targetId = this.getAttribute('href');
      if (targetId === '#') return;
      const target = document.querySelector(targetId);
      if (target) {
        e.preventDefault();
        const navHeight = navbar.offsetHeight;
        const targetPosition = target.getBoundingClientRect().top + window.scrollY - navHeight - 20;
        window.scrollTo({ top: targetPosition, behavior: 'smooth' });
      }
    });
  });

  // ---- Scroll reveal animation ----
  function addRevealClasses() {
    const elements = document.querySelectorAll(
      '.feature-card, .screenshot-card, .tech-card, .section-header'
    );
    elements.forEach(function(el) {
      el.classList.add('reveal');
    });
  }

  function revealOnScroll() {
    const reveals = document.querySelectorAll('.reveal');
    const windowHeight = window.innerHeight;

    reveals.forEach(function(el) {
      const elementTop = el.getBoundingClientRect().top;
      const revealPoint = 80;
      if (elementTop < windowHeight - revealPoint) {
        el.classList.add('visible');
      }
    });
  }

  addRevealClasses();
  window.addEventListener('scroll', revealOnScroll, { passive: true });
  revealOnScroll(); // Initial check

  // ---- Download button animation ----
  const downloadBtn = document.getElementById('downloadBtn');

  if (downloadBtn) {
    downloadBtn.addEventListener('click', function() {
      this.style.transform = 'scale(0.95)';
      setTimeout(function() {
        downloadBtn.style.transform = '';
      }, 150);
    });
  }

  // ---- Parallax effect for phone mockup ----
  const phoneMockup = document.querySelector('.phone-mockup');

  if (phoneMockup && window.innerWidth > 768) {
    window.addEventListener('mousemove', function(e) {
      const x = (e.clientX / window.innerWidth - 0.5) * 10;
      const y = (e.clientY / window.innerHeight - 0.5) * 10;
      phoneMockup.style.transform = 'perspective(1000px) rotateY(' + x + 'deg) rotateX(' + (-y) + 'deg)';
    });

    window.addEventListener('mouseleave', function() {
      phoneMockup.style.transform = 'perspective(1000px) rotateY(0deg) rotateX(0deg)';
    });
  }

  // ---- Animated counter for stats (optional) ----
  function animateValue(element, start, end, duration) {
    var startTime = null;
    var isVersion = element.textContent.includes('v');

    function step(timestamp) {
      if (!startTime) startTime = timestamp;
      var progress = Math.min((timestamp - startTime) / duration, 1);
      if (isVersion) {
        element.textContent = 'v' + (start + (end - start) * progress).toFixed(1) + '.0';
      } else {
        element.textContent = Math.floor(start + (end - start) * progress) + '+';
      }
      if (progress < 1) {
        requestAnimationFrame(step);
      }
    }

    requestAnimationFrame(step);
  }

  // Observe hero stats section for animation trigger
  var statsAnimated = false;
  var statsObserver = new IntersectionObserver(function(entries) {
    entries.forEach(function(entry) {
      if (entry.isIntersecting && !statsAnimated) {
        statsAnimated = true;
        var statValues = document.querySelectorAll('.stat-value');
        statValues.forEach(function(el) {
          el.style.opacity = '1';
          el.style.transform = 'translateY(0)';
        });
      }
    });
  }, { threshold: 0.5 });

  var statsSection = document.querySelector('.hero-stats');
  if (statsSection) {
    statsObserver.observe(statsSection);
  }

})();
