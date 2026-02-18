import path from 'node:path'
import { defineConfig } from 'prisma/config'

export default defineConfig({
  schema: path.join(import.meta.dirname, 'prisma', 'schema.prisma'),
  migrate: {
    url: process.env.DATABASE_URL!,
  },
  datasource: {
    url: process.env.DATABASE_URL!,
  },
})
