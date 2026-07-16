import React from 'react';
import * as Icons from 'lucide-react';

interface LucideIconProps {
  name: string;
  className?: string;
  size?: number;
}

export const LucideIcon: React.FC<LucideIconProps> = ({ name, className, size = 20 }) => {
  // Safe lookup for Icon components in lucide-react
  const IconComponent = (Icons as any)[name] || Icons.DollarSign;
  return <IconComponent className={className} size={size} />;
};
