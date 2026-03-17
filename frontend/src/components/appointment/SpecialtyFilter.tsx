import React from "react";
import { SPECIALIZATIONS } from "../../constants/specializations";
import { HiHeart, HiUser, HiLightningBolt, HiEye, HiAcademicCap, HiFire, HiBeaker, HiUsers } from "react-icons/hi";

interface SpecialtyFilterProps {
  selectedSpecialty: string | null;
  onSpecialtyChange: (specialty: string | null) => void;
}

const getSpecialtyIcon = (specialty: string) => {
  const lower = specialty.toLowerCase();
  if (lower.includes("cardiology") || lower.includes("heart")) return <HiHeart className="w-5 h-5" />;
  if (lower.includes("pediatric")) return <HiUsers className="w-5 h-5" />;
  if (lower.includes("endocrin")) return <HiLightningBolt className="w-5 h-5" />;
  if (lower.includes("ophthalm") || lower.includes("eye")) return <HiEye className="w-5 h-5" />;
  if (lower.includes("psych") || lower.includes("mental")) return <HiAcademicCap className="w-5 h-5" />;
  if (lower.includes("pulmon") || lower.includes("lung")) return <HiFire className="w-5 h-5" />;
  if (lower.includes("oncology") || lower.includes("cancer")) return <HiBeaker className="w-5 h-5" />;
  return <HiUser className="w-5 h-5" />;
};

export default function SpecialtyFilter({ selectedSpecialty, onSpecialtyChange }: SpecialtyFilterProps) {
  return (
    <div className="mb-8">
      <h3 className="text-base font-semibold text-white mb-4">Choose category</h3>
      <div className="flex flex-wrap gap-3">
        <button
          type="button"
          onClick={() => onSpecialtyChange(null)}
          className={`px-5 py-3 rounded-xl text-sm font-medium transition-all duration-200 flex items-center space-x-2 ${
            selectedSpecialty === null
              ? "bg-blue-600 text-white shadow-lg shadow-blue-500/30 scale-105"
              : "bg-gray-800 text-gray-300 hover:bg-gray-700 border border-gray-700"
          }`}
        >
          <span>All</span>
        </button>
        {SPECIALIZATIONS.map((specialty) => (
          <button
            key={specialty}
            type="button"
            onClick={() => onSpecialtyChange(specialty === selectedSpecialty ? null : specialty)}
            className={`px-5 py-3 rounded-xl text-sm font-medium transition-all duration-200 flex items-center space-x-2 ${
              selectedSpecialty === specialty
                ? "bg-blue-600 text-white shadow-lg shadow-blue-500/30 scale-105"
                : "bg-gray-800 text-gray-300 hover:bg-gray-700 border border-gray-700"
            }`}
          >
            {getSpecialtyIcon(specialty)}
            <span>{specialty}</span>
          </button>
        ))}
      </div>
    </div>
  );
}

