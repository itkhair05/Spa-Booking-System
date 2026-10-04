/**
 * HeroAtmosphere — Lightweight, Pure CSS Organic Wellness Atmosphere.
 *
 * Replaces Three.js completely with:
 * - Serene multi-stop organic gradients (Warm Ivory, Muted Sage, Champagne)
 * - Three slow-breathing blurred ambient orbs (16s - 22s harmonic cycles)
 * - Delicate concentric botanical lotus halo with subtle champagne sheen
 * - Full prefers-reduced-motion accessibility support
 * - Zero WebGL, zero canvas, zero runtime dependencies, pure GPU acceleration.
 */
export const HeroAtmosphere = () => {
  return (
    <div
      aria-hidden="true"
      className="absolute inset-0 pointer-events-none overflow-hidden select-none -z-10"
    >
      {/* 1. Base Subtle Organic Radial & Linear Gradients */}
      <div
        className="absolute inset-0 opacity-80"
        style={{
          background: `
            radial-gradient(ellipse 70% 55% at 50% 25%, rgba(246, 239, 226, 0.55) 0%, rgba(250, 248, 245, 0) 70%),
            radial-gradient(circle 450px at 85% 15%, rgba(226, 236, 228, 0.45) 0%, rgba(250, 248, 245, 0) 65%),
            radial-gradient(circle 400px at 15% 75%, rgba(246, 239, 226, 0.4) 0%, rgba(250, 248, 245, 0) 60%),
            linear-gradient(to bottom, rgba(250, 248, 245, 0.1) 0%, rgba(250, 248, 245, 0.7) 80%, #faf8f5 100%)
          `,
        }}
      />

      {/* 2. Floating Ambient Organic Orbs (Breathing Aura Blobs) */}
      {/* Orb 1: Champagne & Warm Ivory (Top Right) */}
      <div className="absolute -top-12 right-1/6 w-[420px] sm:w-[540px] h-[420px] sm:h-[540px] rounded-full bg-gradient-to-br from-[#f6efe2]/75 via-[#fcf9f2]/40 to-transparent blur-3xl animate-hero-aura-1" />

      {/* Orb 2: Muted Sage Glow (Center-Left) */}
      <div className="absolute top-1/4 -left-12 w-[360px] sm:w-[480px] h-[360px] sm:h-[480px] rounded-full bg-gradient-to-tr from-[#e2ece4]/65 via-[#c6d8c9]/25 to-transparent blur-3xl animate-hero-aura-2" />

      {/* Orb 3: Champagne Gold Accent (Bottom Center) */}
      <div className="absolute bottom-6 left-1/3 w-[300px] sm:w-[420px] h-[300px] sm:h-[420px] rounded-full bg-gradient-to-t from-[#b8976c]/12 via-[#f6efe2]/35 to-transparent blur-2xl animate-hero-aura-3" />

      {/* 3. Subtle Central Botanical Zen Aura (Delicate Organic Halo) */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[340px] sm:w-[480px] h-[340px] sm:h-[480px] rounded-full border border-[#b8976c]/15 animate-hero-zen flex items-center justify-center">
        <div className="w-[260px] sm:w-[380px] h-[260px] sm:h-[380px] rounded-full border border-[#c6d8c9]/25 flex items-center justify-center">
          <div className="w-[180px] sm:w-[260px] h-[180px] sm:h-[260px] rounded-full border border-[#b8976c]/10" />
        </div>
      </div>
    </div>
  );
};
