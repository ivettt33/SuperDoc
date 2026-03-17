import React from "react";
import { HiSearch } from "react-icons/hi";
import { Input } from "@/components/ui/input";

interface DoctorSearchBarProps {
  searchQuery: string;
  onSearchChange: (query: string) => void;
  placeholder?: string;
}

export default function DoctorSearchBar({ 
  searchQuery, 
  onSearchChange, 
  placeholder = "Search doctors, services..." 
}: DoctorSearchBarProps) {
  return (
    <div className="relative mb-8">
      <HiSearch className="absolute left-5 top-1/2 transform -translate-y-1/2 text-gray-400 w-6 h-6 z-10" />
      <Input
        type="text"
        value={searchQuery}
        onChange={(e) => onSearchChange(e.target.value)}
        placeholder={placeholder}
        className="pl-14 pr-5 py-4 text-base w-full bg-gray-800 border-gray-700 rounded-xl focus:ring-2 focus:ring-blue-500/20"
      />
    </div>
  );
}

