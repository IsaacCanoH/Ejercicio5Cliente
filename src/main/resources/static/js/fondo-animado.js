// Keep the poster when motion is reduced or autoplay is unavailable.
document.querySelectorAll('[data-background-video]').forEach(function (video) {
    const preference = window.matchMedia('(prefers-reduced-motion: reduce)');
    const source = video.querySelector('source[data-src]');
    function updatePlayback() {
        if (preference.matches || document.hidden) {
            video.pause();
            return;
        }
        if (!source.hasAttribute('src')) {
            source.src = source.dataset.src;
            video.load();
        }
        video.muted = true;
        video.play().catch(function () { /* The poster remains visible. */ });
    }
    preference.addEventListener('change', updatePlayback);
    document.addEventListener('visibilitychange', updatePlayback);
    updatePlayback();
});
