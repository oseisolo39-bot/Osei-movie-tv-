/**
 * HlsPlayer Component
 * A responsive, feature-rich HLS video player component.
 */
class HlsPlayer {
    constructor(containerId, options = {}) {
        this.container = document.getElementById(containerId);
        if (!this.container) throw new Error(`Container #${containerId} not found`);
        
        this.options = {
            autoplay: true,
            muted: false,
            controls: true,
            ...options
        };

        this.hls = null;
        this.video = null;
        this.init();
    }

    init() {
        this.container.innerHTML = `
            <div class="relative w-full aspect-video bg-black overflow-hidden group">
                <video id="hls-video-element" class="w-full h-full object-contain" playsinline ${this.options.autoplay ? 'autoplay' : ''} ${this.options.muted ? 'muted' : ''}></video>
                
                <!-- Overlay: Loading -->
                <div id="hls-loader" class="absolute inset-0 flex items-center justify-center bg-black/60 z-10 transition-opacity duration-300">
                    <div class="w-12 h-12 border-4 border-zinc-800 border-t-red-500 rounded-full animate-spin"></div>
                </div>

                <!-- Overlay: Error -->
                <div id="hls-error" class="absolute inset-0 hidden flex flex-col items-center justify-center bg-zinc-950/90 z-20 p-4 text-center">
                    <span class="text-4xl mb-2">⚠️</span>
                    <h3 class="text-white font-bold text-sm">Playback Error</h3>
                    <p class="text-zinc-400 text-xs mt-1">Failed to connect to stream</p>
                    <button id="hls-retry-btn" class="mt-4 px-4 py-2 bg-red-600 text-white text-xs font-bold rounded-lg hover:bg-red-500 transition">Retry</button>
                </div>

                <!-- Custom Controls -->
                <div id="hls-controls" class="absolute bottom-0 left-0 right-0 p-4 bg-gradient-to-t from-black/80 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-center gap-4 z-30">
                    <button id="hls-play-pause" class="text-white hover:text-red-500 transition">
                        <svg class="w-6 h-6" fill="currentColor" viewBox="0 0 24 24"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/></svg>
                    </button>
                    
                    <!-- Volume Control Group -->
                    <div class="flex items-center gap-2 group/volume">
                        <button id="hls-mute-toggle" class="text-white hover:text-red-500 transition">
                            <svg class="w-6 h-6" fill="currentColor" viewBox="0 0 24 24"><path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z"/></svg>
                        </button>
                        <input id="hls-volume-slider" type="range" min="0" max="1" step="0.01" value="1" 
                            class="w-0 group-hover/volume:w-20 md:w-20 h-1 bg-zinc-700 rounded-lg appearance-none cursor-pointer accent-red-600 transition-all duration-300">
                    </div>

                    <div class="flex-grow"></div>
                    <button id="hls-fullscreen" class="text-white hover:text-red-500 transition">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5v-4m0 4h-4m4 0l-5-5"/></svg>
                    </button>
                </div>
            </div>
        `;

        this.video = this.container.querySelector('#hls-video-element');
        this.loader = this.container.querySelector('#hls-loader');
        this.errorOverlay = this.container.querySelector('#hls-error');
        this.retryBtn = this.container.querySelector('#hls-retry-btn');
        this.playPauseBtn = this.container.querySelector('#hls-play-pause');
        this.muteBtn = this.container.querySelector('#hls-mute-toggle');
        this.volumeSlider = this.container.querySelector('#hls-volume-slider');
        this.fullscreenBtn = this.container.querySelector('#hls-fullscreen');

        this.setupListeners();
    }

    setupListeners() {
        this.retryBtn.onclick = () => this.reload();
        
        this.playPauseBtn.onclick = () => {
            if (this.video.paused) this.video.play();
            else this.video.pause();
        };

        this.muteBtn.onclick = () => {
            this.video.muted = !this.video.muted;
            this.updateMuteIcon();
        };

        this.volumeSlider.oninput = (e) => {
            const val = parseFloat(e.target.value);
            this.video.volume = val;
            if (val === 0) {
                this.video.muted = true;
            } else if (this.video.muted) {
                this.video.muted = false;
            }
            this.updateMuteIcon();
        };

        this.fullscreenBtn.onclick = () => this.toggleFullscreen();

        this.video.addEventListener('play', () => this.updatePlayPauseIcon(true));
        this.video.addEventListener('pause', () => this.updatePlayPauseIcon(false));
        this.video.addEventListener('waiting', () => this.showLoader(true));
        this.video.addEventListener('playing', () => this.showLoader(false));
        
        this.video.addEventListener('error', () => {
            this.showError(true);
        });

        this.video.addEventListener('volumechange', () => {
            this.updateMuteIcon();
            this.volumeSlider.value = this.video.muted ? 0 : this.video.volume;
        });
    }

    load(url) {
        if (!url) return;
        this.currentUrl = url;
        this.showError(false);
        this.showLoader(true);

        if (this.hls) {
            this.hls.destroy();
        }

        if (Hls.isSupported()) {
            this.hls = new Hls({
                enableWorker: true,
                lowLatencyMode: true
            });
            this.hls.loadSource(url);
            this.hls.attachMedia(this.video);
            
            this.hls.on(Hls.Events.MANIFEST_PARSED, () => {
                this.video.play().catch(err => console.warn("Autoplay blocked:", err));
                this.showLoader(false);
            });

            this.hls.on(Hls.Events.ERROR, (event, data) => {
                if (data.fatal) {
                    switch (data.type) {
                        case Hls.ErrorTypes.NETWORK_ERROR:
                            this.hls.startLoad();
                            break;
                        case Hls.ErrorTypes.MEDIA_ERROR:
                            this.hls.recoverMediaError();
                            break;
                        default:
                            this.showError(true);
                            break;
                    }
                }
            });
        } else if (this.video.canPlayType('application/vnd.apple.mpegurl')) {
            this.video.src = url;
            this.video.addEventListener('loadedmetadata', () => {
                this.video.play();
                this.showLoader(false);
            });
        } else {
            this.showError(true);
        }
    }

    reload() {
        if (this.currentUrl) this.load(this.currentUrl);
    }

    showLoader(show) {
        if (show) this.loader.classList.remove('opacity-0');
        else this.loader.classList.add('opacity-0');
    }

    showError(show) {
        if (show) this.errorOverlay.classList.remove('hidden');
        else this.errorOverlay.classList.add('hidden');
    }

    updatePlayPauseIcon(playing) {
        this.playPauseBtn.innerHTML = playing 
            ? '<svg class="w-6 h-6" fill="currentColor" viewBox="0 0 24 24"><path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/></svg>'
            : '<svg class="w-6 h-6" fill="currentColor" viewBox="0 0 24 24"><path d="M8 5v14l11-7z"/></svg>';
    }

    updateMuteIcon() {
        this.muteBtn.innerHTML = this.video.muted
            ? '<svg class="w-6 h-6 text-red-500" fill="currentColor" viewBox="0 0 24 24"><path d="M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.21.05-.42.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z"/></svg>'
            : '<svg class="w-6 h-6" fill="currentColor" viewBox="0 0 24 24"><path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"/></svg>';
    }

    toggleFullscreen() {
        if (!document.fullscreenElement) {
            this.container.querySelector('.group').requestFullscreen().catch(err => {
                console.error(`Error attempting to enable full-screen mode: ${err.message}`);
            });
        } else {
            document.exitFullscreen();
        }
    }
}
