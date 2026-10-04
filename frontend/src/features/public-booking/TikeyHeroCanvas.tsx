import { useEffect, useRef, useState } from 'react';
import * as THREE from 'three';

function checkWebGLSupport(): boolean {
  if (typeof window === 'undefined') return true;
  try {
    const testCanvas = document.createElement('canvas');
    return !!(testCanvas.getContext('webgl') || testCanvas.getContext('experimental-webgl'));
  } catch {
    return false;
  }
}

/**
 * TikeyHeroCanvas — Elegant, organic wellness 3D sculpture.
 * Uses native Three.js with soft sage & ivory tones, translucent physical material,
 * restrained morphing wave, viewport intersection pausing, and WebGL error fallback.
 */
export const TikeyHeroCanvas = () => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [hasWebGL] = useState(checkWebGLSupport);

  useEffect(() => {
    if (!hasWebGL) return;

    const container = containerRef.current;
    if (!container) return;

    // 2. Reduced motion check
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const isMobile = window.innerWidth < 768;

    // 3. Scene, Camera, Renderer setup
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(
      45,
      container.clientWidth / (container.clientHeight || 1),
      0.1,
      100
    );
    camera.position.set(0, 0, 4.8);

    const renderer = new THREE.WebGLRenderer({
      alpha: true,
      antialias: !isMobile,
      powerPreference: 'low-power',
    });
    renderer.setSize(container.clientWidth, container.clientHeight);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, isMobile ? 1 : 1.75));
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.1;

    container.appendChild(renderer.domElement);

    // 4. Lighting — soft champagne & sage wellness glow
    const ambientLight = new THREE.AmbientLight(0xfcf9f2, 1.2);
    scene.add(ambientLight);

    const keyLight = new THREE.DirectionalLight(0xe2ece4, 2.0);
    keyLight.position.set(3, 4, 3);
    scene.add(keyLight);

    const fillLight = new THREE.DirectionalLight(0xb8976c, 1.4);
    fillLight.position.set(-3, -2, 2);
    scene.add(fillLight);

    const backLight = new THREE.PointLight(0x566f5c, 1.0, 10);
    backLight.position.set(0, -3, -2);
    scene.add(backLight);

    // 5. Geometry & Organic Deformation
    const detail = isMobile ? 24 : 48;
    const geometry = new THREE.SphereGeometry(1.4, detail, detail);
    const originalPositions = new Float32Array(geometry.attributes.position.array);

    // Soft pearl / translucent liquid material
    const material = new THREE.MeshPhysicalMaterial({
      color: 0xf4f0e8,
      emissive: 0x28362c,
      emissiveIntensity: 0.08,
      roughness: 0.28,
      metalness: 0.05,
      clearcoat: 0.6,
      clearcoatRoughness: 0.2,
      transmission: 0.25,
      ior: 1.35,
      transparent: true,
      opacity: 0.92,
      wireframe: false,
    });

    const mesh = new THREE.Mesh(geometry, material);
    scene.add(mesh);

    // Subtle orbiting halo ring
    const ringGeometry = new THREE.TorusGeometry(1.9, 0.015, 12, isMobile ? 32 : 64);
    const ringMaterial = new THREE.MeshBasicMaterial({
      color: 0xb8976c,
      transparent: true,
      opacity: 0.35,
    });
    const ring = new THREE.Mesh(ringGeometry, ringMaterial);
    ring.rotation.x = Math.PI * 0.45;
    ring.rotation.y = Math.PI * 0.15;
    scene.add(ring);

    // 6. Interaction & Animation Variables
    let animationFrameId: number;
    const clock = new THREE.Clock();
    let targetRotationX = 0;
    let targetRotationY = 0;
    let isVisibleInViewport = true;

    // Mouse movement listener (subtle organic parallax)
    const handleMouseMove = (e: MouseEvent) => {
      if (prefersReducedMotion || isMobile) return;
      const { innerWidth, innerHeight } = window;
      const x = (e.clientX / innerWidth) * 2 - 1;
      const y = -(e.clientY / innerHeight) * 2 + 1;
      targetRotationY = x * 0.35;
      targetRotationX = y * 0.25;
    };

    window.addEventListener('mousemove', handleMouseMove, { passive: true });

    // Viewport Intersection Observer (pause rendering when Hero is scrolled out)
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          isVisibleInViewport = entry.isIntersecting;
        });
      },
      { threshold: 0.05 }
    );
    observer.observe(container);

    // 7. Animation Loop
    const posAttribute = geometry.attributes.position;
    const vertex = new THREE.Vector3();

    const animate = () => {
      animationFrameId = requestAnimationFrame(animate);

      if (!isVisibleInViewport) return;

      const elapsedTime = clock.getElapsedTime();

      // Smooth inertia rotation
      mesh.rotation.y += (targetRotationY - mesh.rotation.y) * 0.04;
      mesh.rotation.x += (targetRotationX - mesh.rotation.x) * 0.04;
      ring.rotation.z += 0.002;

      if (!prefersReducedMotion) {
        // Continuous slow organic rotation
        mesh.rotation.y += 0.003;
        mesh.rotation.z += 0.0015;

        // Wave deformation across vertices
        const speed = elapsedTime * 0.8;
        const positions = posAttribute.array as Float32Array;

        for (let i = 0; i < positions.length; i += 3) {
          const u = originalPositions[i];
          const v = originalPositions[i + 1];
          const w = originalPositions[i + 2];

          vertex.set(u, v, w).normalize();

          // Multi-frequency harmonic swell
          const wave1 = Math.sin(vertex.x * 2.5 + speed) * 0.12;
          const wave2 = Math.cos(vertex.y * 3.0 + speed * 1.2) * 0.08;
          const wave3 = Math.sin(vertex.z * 2.0 + speed * 0.9) * 0.06;
          const displacement = 1.4 + wave1 + wave2 + wave3;

          positions[i] = vertex.x * displacement;
          positions[i + 1] = vertex.y * displacement;
          positions[i + 2] = vertex.z * displacement;
        }

        posAttribute.needsUpdate = true;
        geometry.computeVertexNormals();
      }

      renderer.render(scene, camera);
    };

    if (prefersReducedMotion) {
      renderer.render(scene, camera);
    } else {
      animate();
    }

    // 8. Responsive Resize
    const handleResize = () => {
      if (!container) return;
      const width = container.clientWidth;
      const height = container.clientHeight || 1;
      camera.aspect = width / height;
      camera.updateProjectionMatrix();
      renderer.setSize(width, height);
    };

    window.addEventListener('resize', handleResize);

    // 9. Cleanup & Resource Disposal
    return () => {
      cancelAnimationFrame(animationFrameId);
      observer.disconnect();
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('resize', handleResize);

      geometry.dispose();
      material.dispose();
      ringGeometry.dispose();
      ringMaterial.dispose();
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
      {hasWebGL === false && (
        <div className="w-80 h-80 rounded-full bg-gradient-to-tr from-[#c6d8c9]/50 via-[#f2f6f3]/80 to-[#f6efe2]/60 blur-2xl animate-pulse" />
      )}
    </div>
  );
};
