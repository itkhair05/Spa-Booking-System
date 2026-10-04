import { useEffect, useRef, useState } from 'react';
import * as THREE from 'three';

function checkWebGLSupport(): boolean {
  if (typeof window === 'undefined') return true;
  try {
    const testCanvas = document.createElement('canvas');
    return !!(
      testCanvas.getContext('webgl') ||
      testCanvas.getContext('experimental-webgl')
    );
  } catch {
    return false;
  }
}

/**
 * Creates an organic, gracefully curved petal geometry anchored at base (0, 0, 0).
 * Curvature and cupping are computed smoothly along the petal profile.
 */
function createPetalGeometry(
  width: number,
  length: number,
  curvature: number,
  cupDepth: number,
  segmentsX = 16,
  segmentsY = 24
): THREE.BufferGeometry {
  const geom = new THREE.PlaneGeometry(width, length, segmentsX, segmentsY);
  const pos = geom.attributes.position;

  for (let i = 0; i < pos.count; i++) {
    let x = pos.getX(i);
    // Shift Y so origin (0,0,0) is at the stem/base of the petal
    const y = pos.getY(i) + length / 2;
    let z = pos.getZ(i);

    const normY = Math.max(0, Math.min(1, y / length)); // 0 (base) to 1 (tip)

    // Natural petal contour: tapered at base, widest around 55-60%, tapered softly at tip
    const contourWidth = Math.sin(Math.pow(normY, 0.72) * Math.PI);
    x *= contourWidth;

    // Longitudinal curl: curving backward/outward as it blooms
    z += -curvature * Math.pow(normY, 1.85);

    // Transverse cupping: spoon-like organic scoop
    const normX = Math.abs(x) / (width * 0.5 + 0.0001);
    z += cupDepth * (1 - Math.min(1, normX * normX)) * Math.sin(normY * Math.PI);

    pos.setXYZ(i, x, y, z);
  }

  geom.computeVertexNormals();
  return geom;
}

interface PetalRing {
  mesh: THREE.Mesh;
  pivot: THREE.Group;
  baseAngle: number;
  openAngle: number;
  bloomMultiplier: number;
}

/**
 * TikeyHeroCanvas — Blooming 3D Flower Sculpture.
 * Replaces the old rotating planet with an organic, elegant flower sculpture
 * that slowly unfurls its petals, breathes softly, and returns gracefully toward rest.
 *
 * Palette: Muted Sage (#566F5C / #c6d8c9), Warm Ivory (#FAF8F5 / #f7f4ec), Champagne (#B8976C).
 */
export const TikeyHeroCanvas = () => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [hasWebGL] = useState(checkWebGLSupport);

  useEffect(() => {
    if (!hasWebGL) return;

    const container = containerRef.current;
    if (!container) return;

    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const isMobile = window.innerWidth < 768;

    // 1. Scene, Camera, Renderer
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(
      42,
      container.clientWidth / (container.clientHeight || 1),
      0.1,
      100
    );
    // Position camera slightly elevated to look gracefully into the blooming flower
    camera.position.set(0, 0.8, 5.2);
    camera.lookAt(0, 0.2, 0);

    const renderer = new THREE.WebGLRenderer({
      alpha: true,
      antialias: !isMobile,
      powerPreference: 'low-power',
    });
    renderer.setSize(container.clientWidth, container.clientHeight);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, isMobile ? 1 : 1.75));
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.08;

    container.appendChild(renderer.domElement);

    // 2. Curated Wellness Lighting
    // Warm ambient light (ivory soft tone)
    const ambientLight = new THREE.AmbientLight(0xfcfaf4, 1.3);
    scene.add(ambientLight);

    // Warm champagne key light
    const keyLight = new THREE.DirectionalLight(0xf6ebd9, 2.2);
    keyLight.position.set(3.5, 4.5, 3.5);
    scene.add(keyLight);

    // Soft sage fill light
    const fillLight = new THREE.DirectionalLight(0xdbe7dd, 1.4);
    fillLight.position.set(-3.5, -2, 2.5);
    scene.add(fillLight);

    // Subtle champagne rim light behind the flower
    const rimLight = new THREE.PointLight(0xb8976c, 1.2, 12);
    rimLight.position.set(0, -2, -3);
    scene.add(rimLight);

    // 3. Root Flower Group
    const flowerGroup = new THREE.Group();
    // Tilt the flower toward the viewer slightly
    flowerGroup.rotation.x = 0.28;
    scene.add(flowerGroup);

    // 4. Materials
    // DoubleSide is essential for blooming petals so both outer and inner curves render cleanly
    const outerMaterial = new THREE.MeshPhysicalMaterial({
      color: 0xded8cc, // subtle muted sage-taupe
      roughness: 0.38,
      metalness: 0.04,
      clearcoat: 0.25,
      clearcoatRoughness: 0.2,
      transmission: 0.12,
      transparent: true,
      opacity: 0.94,
      side: THREE.DoubleSide,
    });

    const midMaterial = new THREE.MeshPhysicalMaterial({
      color: 0xf3eee3, // warm ivory
      roughness: 0.32,
      metalness: 0.03,
      clearcoat: 0.35,
      clearcoatRoughness: 0.18,
      transmission: 0.16,
      transparent: true,
      opacity: 0.96,
      side: THREE.DoubleSide,
    });

    const innerMaterial = new THREE.MeshPhysicalMaterial({
      color: 0xfcf9f2, // pure luminous ivory with champagne warmth
      roughness: 0.28,
      metalness: 0.02,
      clearcoat: 0.45,
      clearcoatRoughness: 0.15,
      transmission: 0.2,
      transparent: true,
      opacity: 0.98,
      side: THREE.DoubleSide,
    });

    const stamenMaterial = new THREE.MeshPhysicalMaterial({
      color: 0xb8976c, // brand champagne gold
      emissive: 0x3d2b15,
      emissiveIntensity: 0.2,
      roughness: 0.22,
      metalness: 0.35,
      clearcoat: 0.5,
    });

    // 5. Build Petal Layers
    const petals: PetalRing[] = [];
    const geometriesToDispose: THREE.BufferGeometry[] = [];

    // Layer 1 (Outer Ring): 8 large petals
    const outerCount = isMobile ? 6 : 8;
    const outerGeom = createPetalGeometry(1.15, 2.25, 0.45, 0.28, isMobile ? 12 : 18, isMobile ? 18 : 26);
    geometriesToDispose.push(outerGeom);

    for (let i = 0; i < outerCount; i++) {
      const pivot = new THREE.Group();
      pivot.rotation.y = (i / outerCount) * Math.PI * 2;

      const mesh = new THREE.Mesh(outerGeom, outerMaterial);
      mesh.castShadow = false;
      mesh.receiveShadow = false;
      pivot.add(mesh);

      flowerGroup.add(pivot);
      petals.push({
        mesh,
        pivot,
        baseAngle: 0.35, // ~20 deg
        openAngle: 1.15, // ~66 deg
        bloomMultiplier: 1.0,
      });
    }

    // Layer 2 (Middle Ring): 6 mid petals
    const midCount = isMobile ? 5 : 6;
    const midGeom = createPetalGeometry(0.95, 1.9, 0.38, 0.25, isMobile ? 12 : 16, isMobile ? 16 : 24);
    geometriesToDispose.push(midGeom);
    const midOffset = Math.PI / midCount;

    for (let i = 0; i < midCount; i++) {
      const pivot = new THREE.Group();
      pivot.rotation.y = (i / midCount) * Math.PI * 2 + midOffset;

      const mesh = new THREE.Mesh(midGeom, midMaterial);
      pivot.add(mesh);

      flowerGroup.add(pivot);
      petals.push({
        mesh,
        pivot,
        baseAngle: 0.2, // ~11 deg
        openAngle: 0.82, // ~47 deg
        bloomMultiplier: 0.9,
      });
    }

    // Layer 3 (Inner Ring): 5 delicate petals
    const innerCount = 5;
    const innerGeom = createPetalGeometry(0.75, 1.5, 0.32, 0.22, isMobile ? 10 : 14, isMobile ? 14 : 20);
    geometriesToDispose.push(innerGeom);
    const innerOffset = Math.PI / innerCount + 0.35;

    for (let i = 0; i < innerCount; i++) {
      const pivot = new THREE.Group();
      pivot.rotation.y = (i / innerCount) * Math.PI * 2 + innerOffset;

      const mesh = new THREE.Mesh(innerGeom, innerMaterial);
      pivot.add(mesh);

      flowerGroup.add(pivot);
      petals.push({
        mesh,
        pivot,
        baseAngle: 0.08, // ~5 deg
        openAngle: 0.52, // ~30 deg
        bloomMultiplier: 0.78,
      });
    }

    // 6. Stamen Core / Center Pistils (Champagne Wellness Center)
    const stamenCoreGeom = new THREE.SphereGeometry(0.24, isMobile ? 16 : 24, isMobile ? 12 : 18);
    geometriesToDispose.push(stamenCoreGeom);
    const stamenCore = new THREE.Mesh(stamenCoreGeom, stamenMaterial);
    stamenCore.position.y = 0.15;
    flowerGroup.add(stamenCore);

    // Decorative miniature stamen dots
    const stamenCount = isMobile ? 8 : 14;
    const stamenDotGeom = new THREE.SphereGeometry(0.045, 8, 8);
    geometriesToDispose.push(stamenDotGeom);

    for (let i = 0; i < stamenCount; i++) {
      const dot = new THREE.Mesh(stamenDotGeom, stamenMaterial);
      const angle = (i / stamenCount) * Math.PI * 2;
      const radius = 0.28 + (i % 2) * 0.06;
      dot.position.set(Math.cos(angle) * radius, 0.22 + (i % 3) * 0.04, Math.sin(angle) * radius);
      flowerGroup.add(dot);
    }

    // 7. Interactive Parallax & Viewport Observer
    let animationFrameId: number;
    const clock = new THREE.Clock();
    let targetRotationX = 0.28;
    let targetRotationY = 0;
    let isVisibleInViewport = true;

    const handleMouseMove = (e: MouseEvent) => {
      if (prefersReducedMotion || isMobile) return;
      const { innerWidth, innerHeight } = window;
      const x = (e.clientX / innerWidth) * 2 - 1;
      const y = -(e.clientY / innerHeight) * 2 + 1;
      targetRotationY = x * 0.3;
      targetRotationX = 0.28 + y * 0.18;
    };

    window.addEventListener('mousemove', handleMouseMove, { passive: true });

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          isVisibleInViewport = entry.isIntersecting;
        });
      },
      { threshold: 0.05 }
    );
    observer.observe(container);

    // Helper: apply flower pose based on bloom factor (0.0 = resting bud, 1.0 = fully open)
    const updateBloom = (bloomRatio: number, breatheRatio: number) => {
      petals.forEach(({ mesh, baseAngle, openAngle, bloomMultiplier }) => {
        // Individual bloom angle with organic layer staggering
        const angle = baseAngle + (openAngle - baseAngle) * (bloomRatio * bloomMultiplier);
        mesh.rotation.x = angle;

        // Subtle breathing scale pulse
        const breathe = 1 + breatheRatio * 0.025;
        mesh.scale.set(breathe, breathe, breathe);
      });

      // Center stamen subtly pulses with life
      const stamenScale = 1 + breatheRatio * 0.05;
      stamenCore.scale.set(stamenScale, stamenScale, stamenScale);
    };

    // 8. Animation Loop
    if (prefersReducedMotion) {
      // Respect accessibility: flower stays in serene, fully blossomed state without motion
      updateBloom(0.85, 0);
      renderer.render(scene, camera);
    } else {
      const animate = () => {
        animationFrameId = requestAnimationFrame(animate);

        if (!isVisibleInViewport) return;

        const time = clock.getElapsedTime();

        // Slow, elegant continuous yaw rotation
        flowerGroup.rotation.y += 0.002;

        // Smooth parallax inertia towards pointer
        flowerGroup.rotation.x += (targetRotationX - flowerGroup.rotation.x) * 0.04;
        flowerGroup.rotation.y += (targetRotationY - flowerGroup.rotation.y) * 0.03;

        // Blooming cycle: ~12-14 second harmonic wave
        // 0.5 + 0.5 * sin(...) oscillates smoothly between 0 and 1
        const bloomCycle = 0.5 + 0.5 * Math.sin(time * 0.42);

        // Breathing cycle: faster micro-breathing oscillation (~3 seconds)
        const breatheCycle = Math.sin(time * 1.8);

        updateBloom(bloomCycle, breatheCycle);

        renderer.render(scene, camera);
      };

      animate();
    }

    // 9. Resize Handling
    const handleResize = () => {
      if (!container) return;
      const width = container.clientWidth;
      const height = container.clientHeight || 1;
      camera.aspect = width / height;
      camera.updateProjectionMatrix();
      renderer.setSize(width, height);
    };

    window.addEventListener('resize', handleResize);

    // 10. Cleanup
    return () => {
      cancelAnimationFrame(animationFrameId);
      observer.disconnect();
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('resize', handleResize);

      geometriesToDispose.forEach((g) => g.dispose());
      outerMaterial.dispose();
      midMaterial.dispose();
      innerMaterial.dispose();
      stamenMaterial.dispose();
      renderer.dispose();

      if (container.contains(renderer.domElement)) {
        container.removeChild(renderer.domElement);
      }
    };
  }, [hasWebGL]);

  return (
    <div
      ref={containerRef}
      aria-hidden="true"
      className="absolute inset-0 pointer-events-none flex items-center justify-center overflow-hidden z-0"
    >
      {/* Fallback graceful visual if WebGL is disabled or failed */}
      {!hasWebGL && (
        <div className="relative w-72 h-72 sm:w-96 sm:h-96 flex items-center justify-center">
          {/* Layered botanical petal gradient rings */}
          <div className="absolute inset-0 rounded-full bg-gradient-to-tr from-[#c6d8c9]/40 via-[#f2f6f3]/60 to-[#f6efe2]/50 blur-3xl animate-pulse" />
          <div className="relative w-48 h-48 rounded-full border border-[#b8976c]/40 bg-white/40 backdrop-blur-sm flex items-center justify-center shadow-lg">
            <svg
              viewBox="0 0 100 100"
              className="w-28 h-28 text-[#b8976c] opacity-80"
              fill="currentColor"
            >
              <path d="M50 15 C58 35, 75 42, 85 50 C75 58, 58 65, 50 85 C42 65, 25 58, 15 50 C25 42, 42 35, 50 15 Z" opacity="0.7" />
              <circle cx="50" cy="50" r="12" fill="#566f5c" opacity="0.6" />
            </svg>
          </div>
        </div>
      )}
    </div>
  );
};
